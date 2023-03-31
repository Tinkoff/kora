package ru.tinkoff.kora.resilient.symbol.processor.aop

import com.google.devtools.ksp.KspExperimental
import com.google.devtools.ksp.getClassDeclarationByName
import com.google.devtools.ksp.processing.Resolver
import com.google.devtools.ksp.symbol.KSFunctionDeclaration
import com.squareup.kotlinpoet.ClassName
import com.squareup.kotlinpoet.CodeBlock
import com.squareup.kotlinpoet.MemberName
import com.squareup.kotlinpoet.ksp.toClassName
import com.squareup.kotlinpoet.ksp.toTypeName
import reactor.core.publisher.Flux
import reactor.core.publisher.Mono
import ru.tinkoff.kora.aop.symbol.processor.KoraAspect
import ru.tinkoff.kora.ksp.common.FunctionUtils.isFlow
import ru.tinkoff.kora.ksp.common.FunctionUtils.isFlux
import ru.tinkoff.kora.ksp.common.FunctionUtils.isFuture
import ru.tinkoff.kora.ksp.common.FunctionUtils.isMono
import ru.tinkoff.kora.ksp.common.FunctionUtils.isSuspend
import ru.tinkoff.kora.ksp.common.KotlinPoetUtils.controlFlow
import ru.tinkoff.kora.ksp.common.exception.ProcessingErrorException
import java.util.concurrent.Future

@KspExperimental
class RetryableKoraAspect(val resolver: Resolver) : KoraAspect {

    companion object {
        const val ANNOTATION_TYPE: String = "ru.tinkoff.kora.resilient.retry.annotation.Retryable"
        private val CanRetryResult = ClassName("ru.tinkoff.kora.resilient.retry", "Retrier", "RetryState", "CanRetryResult")
        private val CanRetry = CanRetryResult.nestedClass("CanRetry")
        private val CantRetry = CanRetryResult.nestedClass("CantRetry")
        private val RetryExhausted = CanRetryResult.nestedClass("RetryExhausted")
        private val delayMember = MemberName("kotlinx.coroutines", "delay")
        private val timeMember = MemberName("kotlin.time.Duration.Companion", "nanoseconds")
        private val flowMember = MemberName("kotlinx.coroutines.flow", "flow")
        private val emitMember = MemberName("kotlinx.coroutines.flow", "emitAll")
        private val retryMember = MemberName("kotlinx.coroutines.flow", "retryWhen")
    }

    override fun getSupportedAnnotationTypes(): Set<String> {
        return setOf(ANNOTATION_TYPE)
    }

    override fun apply(method: KSFunctionDeclaration, superCall: String, aspectContext: KoraAspect.AspectContext): KoraAspect.ApplyResult {
        if (method.isFuture()) {
            throw ProcessingErrorException("@Retryable can't be applied for types assignable from ${Future::class.java}", method)
        } else if (method.isMono()) {
            throw ProcessingErrorException("@Retryable can't be applied for types assignable from ${Mono::class.java}", method)
        } else if (method.isFlux()) {
            throw ProcessingErrorException("@Retryable can't be applied for types assignable from ${Flux::class.java}", method)
        }

        val annotation = method.annotations.filter { a -> a.annotationType.resolve().toClassName().canonicalName == ANNOTATION_TYPE }.first()
        val retryableName = annotation.arguments.asSequence().filter { arg -> arg.name!!.getShortName() == "value" }.map { arg -> arg.value.toString() }.first()

        val managerType = resolver.getClassDeclarationByName("ru.tinkoff.kora.resilient.retry.RetrierManager")!!.asType(listOf())
        val fieldManager = aspectContext.fieldFactory.constructorParam(managerType, listOf())
        val retrierType = resolver.getClassDeclarationByName("ru.tinkoff.kora.resilient.retry.Retrier")!!.asType(listOf())
        val fieldRetrier = aspectContext.fieldFactory.constructorInitialized(
            retrierType,
            CodeBlock.of("%L[%S]", fieldManager, retryableName)
        )

        val body = if (method.isFlow()) {
            val metricType = resolver.getClassDeclarationByName("ru.tinkoff.kora.resilient.retry.telemetry.RetryMetrics")!!.asType(listOf()).makeNullable()
            val fieldMetric = aspectContext.fieldFactory.constructorParam(metricType, listOf())
            buildBodyFlow(method, superCall, retryableName, fieldRetrier, fieldMetric)
        } else if (method.isSuspend()) {
            val metricType = resolver.getClassDeclarationByName("ru.tinkoff.kora.resilient.retry.telemetry.RetryMetrics")!!.asType(listOf()).makeNullable()
            val fieldMetric = aspectContext.fieldFactory.constructorParam(metricType, listOf())
            buildBodySync(method, superCall, retryableName, fieldRetrier, fieldMetric)
        } else {
            buildBodySync(method, superCall, retryableName, fieldRetrier, null)
        }

        return KoraAspect.ApplyResult.MethodBody(body)
    }

    private fun buildBodySync(method: KSFunctionDeclaration, superCall: String, retryName: String, fieldRetrier: String, fieldMetric: String?) = CodeBlock.builder()
        .add("return %L.asState()", fieldRetrier).indent().add("\n")
        .controlFlow(".use { _retryState ->", fieldRetrier) {
            addStatement("var _cause: Exception? = null")
            addStatement("lateinit var _result: %T", method.returnType?.resolve()?.toTypeName())
            controlFlow("while (true)") {
                controlFlow("try") {
                    add("_result = ").add(buildMethodCall(method, superCall)).add("\n")
                    addStatement("break")
                    nextControlFlow("catch (_e: Exception)")
                    addStatement("val _retry = _retryState.canRetry(_e)")
                    controlFlow("if (_retry is %T)", CantRetry) {
                        addStatement("if (_cause != null) _e.addSuppressed(_cause)")
                        addStatement("throw _e")
                    }
                    controlFlow("if (_retry is %T)", RetryExhausted) {
                        addStatement("val _exhaustedException = _retry.toException()")
                        addStatement("if (_cause != null) _exhaustedException.addSuppressed(_cause)")
                        addStatement("throw _exhaustedException")
                    }
                    controlFlow("if (_retry is %T)", CanRetry) {
                        controlFlow("if (_cause == null)") {
                            addStatement("_cause = _e")
                            nextControlFlow("else")
                            addStatement("_cause.addSuppressed(_e)")
                        }
                        if (method.isSuspend()) {
                            addStatement("val _delay = _retryState.delayNanos")
                            addStatement("%L?.recordAttempt(%S, _delay)", fieldMetric, retryName)
                            addStatement("%M(_delay.%M)", delayMember, timeMember)
                        } else {
                            addStatement("_retryState.doDelay()")
                        }
                    }
                }
            }
            addStatement("return@use _result")
        }
        .unindent()
        .add("\n")
        .build()


    private fun buildBodyFlow(method: KSFunctionDeclaration, superCall: String, retryName: String, fieldRetrier: String, fieldMetric: String) =
        CodeBlock.builder().controlFlow("return %M", flowMember) {
            controlFlow("return@flow %L.asState().use { _retryState ->", fieldRetrier) {
                add("%M (", emitMember).indent()
                add(buildMethodCall(method, superCall)).add(".").controlFlow("%M { _cause, _ ->", retryMember) {
                    addStatement("val _retry = _retryState.canRetry(_cause)")
                    controlFlow("if (_retry is %T)", RetryExhausted) {
                        addStatement("throw _retry.toException()")
                    }
                    controlFlow("if (_retry is %T)", CanRetry) {
                        addStatement("val _delay = _retryState.delayNanos")
                        addStatement("%L?.recordAttempt(%S, _delay)", fieldMetric, retryName)
                        addStatement("%M(_delay.%M)", delayMember, timeMember)
                        addStatement("true")
                        nextControlFlow("else")
                        addStatement("false")
                    }
                }
                unindent().add("\n").add(")\n")
            }
        }
            .build()

    private fun buildMethodCall(method: KSFunctionDeclaration, call: String): CodeBlock {
        return CodeBlock.of(method.parameters.asSequence().map { p -> CodeBlock.of("%L", p) }.joinToString(", ", "$call(", ")"))
    }
}
