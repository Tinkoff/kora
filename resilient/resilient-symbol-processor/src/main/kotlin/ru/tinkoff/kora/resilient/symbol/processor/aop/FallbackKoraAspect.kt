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
class FallbackKoraAspect(val resolver: Resolver) : KoraAspect {

    companion object {
        const val ANNOTATION_TYPE: String = "ru.tinkoff.kora.resilient.fallback.annotation.Fallback"
    }

    override fun getSupportedAnnotationTypes(): Set<String> {
        return setOf(ANNOTATION_TYPE)
    }

    override fun apply(method: KSFunctionDeclaration, superCall: String, aspectContext: KoraAspect.AspectContext): KoraAspect.ApplyResult {
        if (method.isFuture()) {
            throw ProcessingErrorException(
                ProcessingError(
                    "@Fallback can't be applied for types assignable from ${Future::class.java}", method, Diagnostic.Kind.NOTE
                )
            )
        }
        if (method.isMono()) {
            throw ProcessingErrorException(
                ProcessingError(
                    "@Fallback can't be applied for types assignable from ${Mono::class.java}", method, Diagnostic.Kind.NOTE
                )
            )
        }
        if (method.isFlux()) {
            throw ProcessingErrorException(
                ProcessingError(
                    "@Fallback can't be applied for types assignable from ${Flux::class.java}", method, Diagnostic.Kind.NOTE
                )
            )
        }

        val annotation = method.annotations.asSequence().filter { a -> a.annotationType.resolve().toClassName().canonicalName == ANNOTATION_TYPE }.first()
        val fallbackName = annotation.arguments.asSequence().filter { arg -> arg.name!!.getShortName() == "value" }.map { arg -> arg.value.toString() }.first()
        val fallback = annotation.asFallback(method)

        val managerType = resolver.getClassDeclarationByName("ru.tinkoff.kora.resilient.fallback.FallbackerManager")!!.asType(listOf())
        val fieldManager = aspectContext.fieldFactory.constructorParam(managerType, listOf())

        val body = if (method.isFlow()) {
            buildBodyFlow(method, fallback, superCall, fallbackName, fieldManager)
        } else if (method.isSuspend()) {
            buildBodySuspend(method, fallback, superCall, fallbackName, fieldManager)
        } else {
            buildBodySync(method, fallback, superCall, fallbackName, fieldManager)
        }

        return KoraAspect.ApplyResult.MethodBody(body)
    }

    private fun buildBodySync(
        method: KSFunctionDeclaration, fallbackCall: FallbackMeta, superCall: String, fallbackName: String, fieldManager: String
    ): CodeBlock {
        if (method.isVoid()) {
            val runnableMember = MemberName("java.lang", "Runnable")
            val superMethod = buildMethodCall(method, superCall)
            return CodeBlock.builder().add(
                """
                  val _fallbacker = %L.get("%L")
                  return _fallbacker.fallback(%M { %L }, %M { %L })
                  """.trimIndent(), fieldManager, fallbackName, runnableMember, superMethod.toString(), runnableMember, fallbackCall.call()
            ).build()
        }

        val supplierMember = MemberName("java.util.function", "Supplier")
        val superMethod = buildMethodSupplier(method, superCall)
        val fallbackSupplier = CodeBlock.of("%M { %L }", supplierMember, fallbackCall.call())
        return CodeBlock.builder().add(
            """
                  val _fallbacker = %L.get("%L")
                  return _fallbacker.fallback(%L, %L)
                  """.trimIndent(), fieldManager, fallbackName, superMethod.toString(), fallbackSupplier
        ).build()
    }

    private fun buildBodySuspend(
        method: KSFunctionDeclaration, fallbackCall: FallbackMeta, superCall: String, fallbackName: String, fieldManager: String
    ): CodeBlock {
        val superMethod = buildMethodCall(method, superCall)
        val prefix = if (method.isVoid()) "" else "return "
        return CodeBlock.builder().add(
            """
                val _fallbacker = %L.get("%L")
                ${prefix}try {
                    %L
                } catch (e: Throwable) {
                    if(_fallbacker.canFallback(e)) {
                        %L
                    } else {
                        throw e
                    }
                }
                """.trimIndent(), fieldManager, fallbackName, superMethod.toString(), fallbackCall.call()
        ).build()
    }

    private fun buildBodyFlow(
        method: KSFunctionDeclaration, fallbackCall: FallbackMeta, superCall: String, fallbackName: String, fieldManager: String
    ): CodeBlock {
        val flowMember = MemberName("kotlinx.coroutines.flow", "flow")
        val catchMember = MemberName("kotlinx.coroutines.flow", "catch")
        val emitMember = MemberName("kotlinx.coroutines.flow", "emitAll")
        val superMethod = buildMethodCall(method, superCall)
        return CodeBlock.builder().add(
            """
                val _fallbacker = %L["%L"]

                return %M {
                    %M(%L)
                }.%M { e ->
                    if (_fallbacker.canFallback(e)) {
                        %M(%L)
                    } else {
                        throw e
                    }
                }
                """.trimIndent(), fieldManager, fallbackName, flowMember, emitMember, superMethod.toString(), catchMember, emitMember, fallbackCall.call()
        ).build()
    }

    private fun buildMethodCall(method: KSFunctionDeclaration, call: String): CodeBlock {
        return CodeBlock.of(method.parameters.asSequence().map { p -> CodeBlock.of("%L", p) }.joinToString(", ", "$call(", ")"))
    }

    private fun buildMethodSupplier(method: KSFunctionDeclaration, call: String): CodeBlock {
        val supplierMember = MemberName("java.util.function", "Supplier")
        return CodeBlock.builder().add("%M { %L }", supplierMember, buildMethodCall(method, call)).build()
    }
}
