package ru.tinkoff.kora.cache.symbol.processor

import com.google.devtools.ksp.KspExperimental
import com.google.devtools.ksp.getAnnotationsByType
import com.google.devtools.ksp.symbol.KSAnnotation
import com.google.devtools.ksp.symbol.KSFunctionDeclaration
import com.squareup.kotlinpoet.asClassName
import com.squareup.kotlinpoet.ksp.toClassName
import ru.tinkoff.kora.cache.annotation.*
import ru.tinkoff.kora.ksp.common.exception.ProcessingError
import ru.tinkoff.kora.ksp.common.exception.ProcessingErrorException
import java.util.regex.Pattern
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
            } else if (puts.isNotEmpty()) {
            } else if (invalidates.isNotEmpty()) {
            }

            throw IllegalStateException("None of $ANNOTATIONS cache annotations found")
        }

        private fun getCacheOperation(method: KSFunctionDeclaration, type: CacheOperation.Type, annotations: List<KSAnnotation>) : CacheOperation {
            val className = method.parentDeclaration?.simpleName?.asString() ?: ""
            val methodName = method.qualifiedName.toString()
            val origin = CacheOperation.Origin(className, methodName)

            val parameters = mutableListOf<List<String>>()
            for (i in annotations.indices) {
                val annotation = annotations[i]

                val annotationParameters = annotation.arguments.firstOrNull{ a -> a.name!!.getQualifier() == "parameters" }
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
//                parameters.add(annotationParameters)
            }

            return CacheOperation(type, listOf(), parameters[0], origin)
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
