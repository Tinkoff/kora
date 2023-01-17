package ru.tinkoff.kora.resilient.symbol.processor.aop

import com.google.devtools.ksp.KspExperimental
import com.google.devtools.ksp.getClassDeclarationByName
import com.google.devtools.ksp.processing.Resolver
import com.google.devtools.ksp.symbol.KSFunctionDeclaration
import com.squareup.kotlinpoet.CodeBlock
import com.squareup.kotlinpoet.MemberName
import com.squareup.kotlinpoet.ksp.toClassName
import reactor.core.publisher.Flux
import reactor.core.publisher.Mono
import ru.tinkoff.kora.aop.symbol.processor.KoraAspect
import ru.tinkoff.kora.ksp.common.FunctionUtils.isFlow
import ru.tinkoff.kora.ksp.common.FunctionUtils.isFlux
import ru.tinkoff.kora.ksp.common.FunctionUtils.isFuture
import ru.tinkoff.kora.ksp.common.FunctionUtils.isMono
import ru.tinkoff.kora.ksp.common.FunctionUtils.isSuspend
import ru.tinkoff.kora.ksp.common.FunctionUtils.isVoid
import ru.tinkoff.kora.ksp.common.exception.ProcessingError
import ru.tinkoff.kora.ksp.common.exception.ProcessingErrorException
import java.util.concurrent.Future
import javax.tools.Diagnostic

@KspExperimental
class RetryableKoraAspect(val resolver: Resolver) : KoraAspect {

    companion object {
        const val ANNOTATION_TYPE: String = "ru.tinkoff.kora.resilient.retry.annotation.Retryable"
    }

    override fun getSupportedAnnotationTypes(): Set<String> {
        return setOf(ANNOTATION_TYPE)
    }

    override fun apply(method: KSFunctionDeclaration, superCall: String, aspectContext: KoraAspect.AspectContext): KoraAspect.ApplyResult {
        if (method.isFuture()) {
            throw ProcessingErrorException(
                ProcessingError(
                    "@Retryable can't be applied for types assignable from ${Future::class.java}", method, Diagnostic.Kind.NOTE
                )
            )
        }
        if (method.isMono()) {
            throw ProcessingErrorException(
                ProcessingError(
                    "@Retryable can't be applied for types assignable from ${Mono::class.java}", method, Diagnostic.Kind.NOTE
                )
            )
        }
        if (method.isFlux()) {
            throw ProcessingErrorException(
                ProcessingError(
                    "@Retryable can't be applied for types assignable from ${Flux::class.java}", method, Diagnostic.Kind.NOTE
                )
            )
        }

        val annotation = method.annotations.asSequence().filter { a -> a.annotationType.resolve().toClassName().canonicalName == ANNOTATION_TYPE }.first()
        val retryableName = annotation.arguments.asSequence().filter { arg -> arg.name!!.getShortName() == "value" }.map { arg -> arg.value.toString() }.first()

        val managerType = resolver.getClassDeclarationByName("ru.tinkoff.kora.resilient.retry.RetrierManager")!!.asType(listOf())
        val fieldManager = aspectContext.fieldFactory.constructorParam(managerType, listOf())
        val retrierType = resolver.getClassDeclarationByName("ru.tinkoff.kora.resilient.retry.Retrier")!!.asType(listOf())
        val fieldRetrier = aspectContext.fieldFactory.constructorInitialized(
            retrierType,
            CodeBlock.of("%L[%S]", fieldManager, retryableName)
        )

        val body = if (method.isFlow()) {
            buildBodyFlow(method, superCall, fieldRetrier)
        } else if (method.isSuspend()) {
            buildBodySuspend(method, superCall, fieldRetrier)
        } else {
            buildBodySync(method, superCall, fieldRetrier)
        }

        return KoraAspect.ApplyResult.MethodBody(body)
    }

    private fun buildBodySync(
        method: KSFunctionDeclaration, superCall: String, retrierName: String
    ): CodeBlock {
        return if (method.isVoid()) {
            CodeBlock.of(
                """
                        val _retrier = %L
                        _retrier.retry { %L }
                        """.trimIndent(), retrierName, buildMethodCall(method, superCall)
            )
        } else {
            CodeBlock.of(
                """
                    val _retrier = %L
                    return _retrier.retry(%L)
                    """.trimIndent(), retrierName, buildMethodSupplier(method, superCall)
            )
        }
    }

    private fun buildBodySuspend(
        method: KSFunctionDeclaration, superCall: String, retrierName: String
    ): CodeBlock {
        val delayMember = MemberName("kotlinx.coroutines", "delay")
        val timeMember = MemberName("kotlin.time.Duration.Companion", "nanoseconds")

        return if (method.isVoid()) {
            CodeBlock.of(
                """
                val _retrier = %L
                val _state = _retrier.asState()
                
                while (true) {
                    try {
                        %L
                    } catch (e: %M) {
                        _state.checkRetry(e)
                        %M(_state.delayNanos.%M)
                    }
                }
                """.trimIndent(), retrierName, buildMethodCall(method, superCall), delayMember, timeMember
            )
        } else {
            CodeBlock.of(
                """
                val _retrier = %L
                val _state = _retrier.asState()
                
                while (true) {
                    try {
                        return %L
                    } catch (e: Exception) {
                        _state.checkRetry(e)
                        %M(_state.delayNanos.%M)
                    }
                }
                """.trimIndent(), retrierName, buildMethodCall(method, superCall), delayMember, timeMember
            )
        }
    }

    private fun buildBodyFlow(
        method: KSFunctionDeclaration, superCall: String, retrierName: String
    ): CodeBlock {
        val flowMember = MemberName("kotlinx.coroutines.flow", "flow")
        val emitMember = MemberName("kotlinx.coroutines.flow", "emitAll")
        val retryMember = MemberName("kotlinx.coroutines.flow", "retryWhen")
        val delayMember = MemberName("kotlinx.coroutines", "delay")
        val timeMember = MemberName("kotlin.time.Duration.Companion", "nanoseconds")

        return CodeBlock.builder().add(
            """
            val _retrier = %L
            
            return %M {
                val _state = _retrier.asState()
                %M(
                    %L.%M{ cause, attempt ->
                        _state.checkRetry(cause)
                        %M(_state.delayNanos.%M)
                        true
                    }
                )
            }
            """.trimIndent(), retrierName, flowMember, emitMember, buildMethodCall(method, superCall), retryMember, delayMember, timeMember
        ).build()
    }

    private fun buildMethodCall(method: KSFunctionDeclaration, call: String): CodeBlock {
        return CodeBlock.of(method.parameters.asSequence().map { p -> CodeBlock.of("%L", p) }.joinToString(", ", "$call(", ")"))
    }

    private fun buildMethodSupplier(method: KSFunctionDeclaration, call: String): CodeBlock {
        val supplierMember = MemberName("java.util.function", "Supplier")
        return CodeBlock.of("%M{ %L }", supplierMember, buildMethodCall(method, call))
    }
}
