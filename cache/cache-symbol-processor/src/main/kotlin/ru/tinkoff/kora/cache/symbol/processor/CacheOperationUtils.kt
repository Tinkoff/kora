package ru.tinkoff.kora.cache.symbol.processor

import com.google.devtools.ksp.KspExperimental
import com.google.devtools.ksp.getAnnotationsByType
import com.google.devtools.ksp.symbol.KSAnnotation
import com.google.devtools.ksp.symbol.KSFunctionDeclaration
import com.google.devtools.ksp.symbol.KSType
import com.squareup.kotlinpoet.asClassName
import com.squareup.kotlinpoet.ksp.toClassName
import ru.tinkoff.kora.cache.annotation.*
import ru.tinkoff.kora.ksp.common.FunctionUtils.isFlow
import ru.tinkoff.kora.ksp.common.FunctionUtils.isFlux
import ru.tinkoff.kora.ksp.common.FunctionUtils.isFuture
import ru.tinkoff.kora.ksp.common.FunctionUtils.isMono
import ru.tinkoff.kora.ksp.common.FunctionUtils.isPublisher
import ru.tinkoff.kora.ksp.common.FunctionUtils.isVoid
import ru.tinkoff.kora.ksp.common.exception.ProcessingError
import ru.tinkoff.kora.ksp.common.exception.ProcessingErrorException
import javax.tools.Diagnostic

@KspExperimental
class CacheOperationUtils {

    companion object {

        private val ANNOTATIONS = setOf(
            Cacheable::class.java, Cacheables::class.java,
            CachePut::class.java, CachePuts::class.java,
            CacheInvalidate::class.java, CacheInvalidates::class.java
        )

        fun getCacheOperation(method: KSFunctionDeclaration): CacheOperation {
            val className = method.parentDeclaration?.simpleName?.asString() ?: ""
            val methodName = method.qualifiedName.toString()
            val origin = CacheOperation.Origin(className, methodName)

            val cacheables = getCacheableAnnotations(method)
            val puts = getCachePutAnnotations(method)
            val invalidates = getCacheInvalidateAnnotations(method)

            val annotations = mutableSetOf<String>()
            cacheables.asSequence().forEach { a -> annotations.add(a.javaClass.canonicalName) }
            puts.asSequence().forEach { a -> annotations.add(a.javaClass.canonicalName) }
            invalidates.asSequence().forEach { a -> annotations.add(a.javaClass.canonicalName) }

            if (annotations.size > 1) {
                throw ProcessingErrorException(
                    ProcessingError(
                        "Expected only one type of Cache annotations but was $annotations for $origin",
                        method,
                        Diagnostic.Kind.ERROR
                    )
                )
            }

            if (cacheables.isNotEmpty()) {
                return getCacheOperation(method, CacheOperation.Type.GET, cacheables)
            } else if (puts.isNotEmpty()) {
                return getCacheOperation(method, CacheOperation.Type.PUT, puts)
            } else if (invalidates.isNotEmpty()) {
                val invalidateAlls = invalidates.asSequence()
                    .flatMap { a -> a.arguments.asSequence() }
                    .filter { a -> a.name!!.getQualifier() == "invalidateAll" }
                    .map { a -> a.value as Boolean }
                    .toList()

                val anyInvalidateAll = invalidateAlls.any { v -> v }
                val allInvalidateAll = invalidateAlls.all { v -> v }

                if (anyInvalidateAll && !allInvalidateAll) {
                    throw ProcessingErrorException(
                        ProcessingError(
                            CacheInvalidate::class.java.toString() + " not all annotations are marked 'invalidateAll' out of all for " + origin,
                            method,
                            Diagnostic.Kind.ERROR,
                        )
                    )
                }

                val type = if(allInvalidateAll) CacheOperation.Type.EVICT_ALL else CacheOperation.Type.EVICT
                return getCacheOperation(method, type, invalidates)
            }

            throw IllegalStateException("None of $ANNOTATIONS cache annotations found")
        }

        private fun getCacheOperation(method: KSFunctionDeclaration, type: CacheOperation.Type, annotations: List<KSAnnotation>): CacheOperation {
            val className = method.parentDeclaration?.simpleName?.asString() ?: ""
            val methodName = method.qualifiedName.toString()
            val origin = CacheOperation.Origin(className, methodName)

            if (type == CacheOperation.Type.GET || type == CacheOperation.Type.PUT) {
                if (method.isVoid()) {
                    throw IllegalArgumentException("@${annotations[0].shortName.getShortName()} annotation can't return Void type, but was for $origin")
                }
            }

            if (method.isMono() || method.isFlux() || method.isPublisher() || method.isFuture() || method.isFlow()) {
                throw IllegalArgumentException("@${annotations[0].shortName.getShortName()} annotation doesn't support return type ${method.returnType} in $origin")
            }

            val cacheImpls = mutableListOf<String>()
            val parameters = mutableListOf<List<String>>()
            for (i in annotations.indices) {
                val annotation = annotations[i]

                val annotationParameters: List<String> = annotation.arguments.filter { a -> a.name!!.getShortName() == "parameters" }
                    .map { it.value as List<String> }
                    .firstOrNull { it.isNotEmpty() }
                    ?: method.parameters.asSequence().map { p -> p.name!!.getShortName() }.toList()

                for (parameter in parameters) {
                    if (parameter != annotationParameters) {
                        throw ProcessingErrorException(
                            ProcessingError(
                                "${annotation.javaClass} parameters mismatch for different annotations for $origin",
                                method,
                                Diagnostic.Kind.ERROR
                            )
                        )
                    }
                }

                val cacheImpl = annotation.arguments.filter { a -> a.name!!.getShortName() == "value" }
                    .map { a -> a.value as KSType }
                    .first()

                parameters.add(annotationParameters)
                cacheImpls.add(cacheImpl.toClassName().canonicalName)
            }

            return CacheOperation(type, cacheImpls, parameters[0], origin)
        }

        private fun getCacheableAnnotations(method: KSFunctionDeclaration): List<KSAnnotation> {
            method.annotations
                .filter { a -> a.annotationType.resolve().toClassName() == Cacheables::class.asClassName() }
                .map { a -> a.arguments[0] }
                .map { arg -> arg.value }
                .toList()

            val annotationAggregate = method.getAnnotationsByType(Cacheables::class).firstOrNull()
            if (annotationAggregate != null) {
                return emptyList()
            }

            return method.annotations
                .filter { a -> a.annotationType.resolve().toClassName() == Cacheable::class.asClassName() }
                .toList()
        }

        private fun getCachePutAnnotations(method: KSFunctionDeclaration): List<KSAnnotation> {
            val annotationAggregate = method.getAnnotationsByType(CachePuts::class).firstOrNull()
            if (annotationAggregate != null) {
                return emptyList()
            }

            return method.annotations
                .filter { a -> a.annotationType.resolve().toClassName() == CachePut::class.asClassName() }
                .toList()
        }

        private fun getCacheInvalidateAnnotations(method: KSFunctionDeclaration): List<KSAnnotation> {
            val annotationAggregate = method.getAnnotationsByType(CacheInvalidates::class).firstOrNull()
            if (annotationAggregate != null) {
                return emptyList()
            }

            return method.annotations
                .filter { a -> a.annotationType.resolve().toClassName() == CacheInvalidate::class.asClassName() }
                .toList()
        }
    }
}
