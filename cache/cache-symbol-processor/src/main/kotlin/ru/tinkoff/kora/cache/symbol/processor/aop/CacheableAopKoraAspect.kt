package ru.tinkoff.kora.cache.symbol.processor.aop

import com.google.devtools.ksp.KspExperimental
import com.google.devtools.ksp.processing.Resolver
import com.google.devtools.ksp.symbol.KSFunctionDeclaration
import com.squareup.kotlinpoet.CodeBlock
import ru.tinkoff.kora.aop.symbol.processor.KoraAspect
import ru.tinkoff.kora.cache.annotation.Cacheable
import ru.tinkoff.kora.cache.annotation.Cacheables
import ru.tinkoff.kora.cache.symbol.processor.CacheOperation
import ru.tinkoff.kora.cache.symbol.processor.CacheOperationUtils.Companion.getCacheOperation
import ru.tinkoff.kora.ksp.common.FunctionUtils.isSuspend

@KspExperimental
class CacheableAopKoraAspect(private val resolver: Resolver) : AbstractAopCacheAspect() {

    override fun getSupportedAnnotationTypes(): Set<String> {
        return setOf(Cacheable::class.java.canonicalName, Cacheables::class.java.canonicalName)
    }

    override fun apply(method: KSFunctionDeclaration, superCall: String, aspectContext: KoraAspect.AspectContext): KoraAspect.ApplyResult {
        val operation = getCacheOperation(method)
        val cacheFields = getCacheFields(operation, resolver, aspectContext)

        val body = if (method.isSuspend()) {
            buildBodySync(method, operation, superCall, cacheFields)
        } else {
            buildBodySync(method, operation, superCall, cacheFields)
        }

        return KoraAspect.ApplyResult.MethodBody(body)
    }

    private fun buildBodySync(
        method: KSFunctionDeclaration,
        operation: CacheOperation,
        superCall: String,
        cacheFields: List<String>
    ): CodeBlock {
        val recordParameters = getKeyRecordParameters(operation, method)
        val superMethod = getSuperMethod(method, superCall)
        val builder = CodeBlock.builder()
        val isSingleNullableParam = operation.parameters.size == 1 && operation.parameters[0].type.resolve().isMarkedNullable

        // cache get
        for (i in cacheFields.indices) {
            val cache = cacheFields[i]
            val prefix = if (i == 0) "var _value = " else "_value = "

            if (isSingleNullableParam) {
                builder.add(prefix)
                    .add("_key?.let { %L.get(it) }\n", cache)
                    .add("if(_value != null) {\n");
            } else {
                builder.add(prefix)
                    .add("%L.get(_key)\n", cache)
                    .add("if(_value != null) {\n");
            }

            for (j in 0 until i) {
                val prevCache = cacheFields[j]
                builder.add("\t%L.put(_key, _value)\n", prevCache)
            }

            builder
                .add("\treturn _value\n")
                .add("}\n\n")
        }

        // cache super method
        builder.add("_value = %L\n", superMethod)

        // cache put
        for (cache in cacheFields) {
            if (isSingleNullableParam) {
                builder.add("_key?.let { %L.put(it, _value) }\n", cache)
            } else {
                builder.add("%L.put(_key, _value)\n", cache)
            }
        }
        builder.add("return _value")

        return if (operation.parameters.size == 1) {
            CodeBlock.builder()
                .add("val _key = %L\n", operation.parameters[0])
                .add(builder.build())
                .build()
        } else {
            CodeBlock.builder()
                .add("val _key = %T.of(%L)\n", getCacheKey(operation), recordParameters)
                .add(builder.build())
                .build()
        }
    }
}
