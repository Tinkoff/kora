package ru.tinkoff.kora.cache.symbol.processor

import com.google.devtools.ksp.KspExperimental
import com.google.devtools.ksp.getAnnotationsByType
import com.google.devtools.ksp.getClassDeclarationByName
import com.google.devtools.ksp.processing.Resolver
import com.google.devtools.ksp.processing.SymbolProcessorEnvironment
import com.google.devtools.ksp.symbol.KSAnnotated
import com.google.devtools.ksp.symbol.KSType
import com.google.devtools.ksp.validate
import com.squareup.kotlinpoet.ClassName
import com.squareup.kotlinpoet.FunSpec
import com.squareup.kotlinpoet.ParameterizedTypeName
import com.squareup.kotlinpoet.TypeName
import com.squareup.kotlinpoet.ksp.toClassName
import ru.tinkoff.kora.cache.annotation.*
import ru.tinkoff.kora.common.Tag
import ru.tinkoff.kora.ksp.common.BaseSymbolProcessor
import ru.tinkoff.kora.ksp.common.FunctionUtils.isFlow
import ru.tinkoff.kora.ksp.common.FunctionUtils.isFlux
import ru.tinkoff.kora.ksp.common.FunctionUtils.isFuture
import ru.tinkoff.kora.ksp.common.FunctionUtils.isMono
import ru.tinkoff.kora.ksp.common.FunctionUtils.isPublisher
import ru.tinkoff.kora.ksp.common.FunctionUtils.isVoid
import ru.tinkoff.kora.ksp.common.exception.ProcessingError
import ru.tinkoff.kora.ksp.common.exception.ProcessingErrorException
import ru.tinkoff.kora.ksp.common.visitFunction
import java.io.IOException
import java.util.*
import java.util.function.Function
import java.util.function.Predicate
import java.util.regex.Pattern
import javax.annotation.processing.RoundEnvironment
import javax.lang.model.element.Element
import javax.lang.model.element.Modifier
import javax.lang.model.element.TypeElement
import javax.lang.model.type.DeclaredType
import javax.lang.model.type.TypeMirror

@KspExperimental
class CacheSymbolProcessor(
    private val environment: SymbolProcessorEnvironment
) : BaseSymbolProcessor(environment) {

    private val NAME_PATTERN = Pattern.compile("^[a-zA-Z][0-9a-zA-Z_]*")

    private val ANNOTATION_CACHE = ClassName("ru.tinkoff.kora.cache.annotation", "Cache")

    private val CLASS_CACHE = ClassName("ru.tinkoff.kora.cache", "Cache")
    private val CLASS_CACHE_TELEMETRY = ClassName("ru.tinkoff.kora.cache.telemetry", "CacheTelemetry")
    private val CLASS_CONFIG = ClassName("com.typesafe.config", "Config")
    private val CLASS_CONFIG_EXTRACTOR = ClassName("ru.tinkoff.kora.config.common.extractor", "ConfigValueExtractor")

    private val CAFFEINE_CACHE = ClassName("ru.tinkoff.kora.cache.caffeine", "CaffeineCache")
    private val CAFFEINE_CACHE_FACTORY = ClassName("ru.tinkoff.kora.cache.caffeine", "CaffeineCacheFactory")
    private val CAFFEINE_CACHE_CONFIG = ClassName("ru.tinkoff.kora.cache.caffeine", "CaffeineCacheConfig")
    private val CAFFEINE_CACHE_IMPL = ClassName("ru.tinkoff.kora.cache.caffeine", "AbstractCaffeineCache")

    private val REDIS_CACHE = ClassName("ru.tinkoff.kora.cache.redis", "RedisCache")
    private val REDIS_CACHE_IMPL = ClassName("ru.tinkoff.kora.cache.redis", "AbstractRedisCache")
    private val REDIS_CACHE_CONFIG = ClassName("ru.tinkoff.kora.cache.redis", "RedisCacheConfig")
    private val REDIS_CACHE_CLIENT_SYNC = ClassName("ru.tinkoff.kora.cache.redis", "SyncRedisClient")
    private val REDIS_CACHE_CLIENT_REACTIVE = ClassName("ru.tinkoff.kora.cache.redis", "ReactiveRedisClient")
    private val REDIS_CACHE_MAPPER_KEY = ClassName("ru.tinkoff.kora.cache.redis", "RedisCacheKeyMapper")
    private val REDIS_CACHE_MAPPER_VALUE = ClassName("ru.tinkoff.kora.cache.redis", "RedisCacheValueMapper")

    private val cacheAnnotations = setOf(
        Cacheable::class, Cacheables::class,
        CachePut::class, CachePuts::class,
        CacheInvalidate::class, CacheInvalidates::class
    )

    override fun processRound(resolver: Resolver): List<KSAnnotated> {
        val symbols = resolver.getSymbolsWithAnnotation(Cacheable::class.qualifiedName!!)
            .plus(resolver.getSymbolsWithAnnotation(Cacheables::class.qualifiedName!!))
            .plus(resolver.getSymbolsWithAnnotation(CachePut::class.qualifiedName!!))
            .plus(resolver.getSymbolsWithAnnotation(CachePuts::class.qualifiedName!!))
            .plus(resolver.getSymbolsWithAnnotation(CacheInvalidate::class.qualifiedName!!))
            .plus(resolver.getSymbolsWithAnnotation(CacheInvalidates::class.qualifiedName!!))
            .toList()

        val symbolsToProcess = symbols.filter { it.validate() }
        symbolsToProcess.forEach {
            it.visitFunction { method ->
                val cacheAnnotations = method.annotations
                    .filter { a ->
                        val canonicalName = a.annotationType.resolve().toClassName().canonicalName
                        cacheAnnotations.any { an -> an.qualifiedName == canonicalName }
                    }.toList()

                if (cacheAnnotations.isNotEmpty()) {
                    try {
                        val annotationNames = cacheAnnotations.map { a -> a.shortName.getShortName() }.toList()
                        val operation = CacheOperationUtils.getCacheOperation(method)

                        if (operation.type == CacheOperation.Type.GET || operation.type == CacheOperation.Type.PUT) {
                            if (method.isVoid()) {
                                throw IllegalArgumentException("$annotationNames annotation can't return Void type, but was for ${operation.origin}")
                            }
                        }

                        if (method.isMono() || method.isFlux() || method.isPublisher() || method.isFuture() || method.isFlow()) {
                            throw IllegalArgumentException("$annotationNames annotation doesn't support return type ${method.returnType} in ${operation.origin}")
                        }


                    } catch (e: IOException) {
                        throw ProcessingErrorException(ProcessingError(e.message.toString(), it));
                    }
                }
            }
        }

        return symbols.filterNot { it.validate() }.toList()
    }

    private fun getCacheSuperType(candidate: TypeElement, resolver: Resolver): Optional<KSType> {
        val caffeineElement = resolver.getClassDeclarationByName(CAFFEINE_CACHE.canonicalName)?.asType(listOf())
        if (caffeineElement != null) {
            return types.directSupertypes(candidate.asType()).stream()
                .filter { t: TypeMirror? -> t is DeclaredType }
                .map<DeclaredType> { t: TypeMirror? -> t as DeclaredType? }
                .filter(Predicate { t: DeclaredType -> types.isAssignable(t.asElement().asType(), caffeineElement.asType()) })
                .findFirst()
        }

        val redisElement = resolver.getClassDeclarationByName(REDIS_CACHE.canonicalName)?.asType(listOf())
        return if (redisElement != null) {
            types.directSupertypes(candidate.asType()).stream()
                .filter { t: TypeMirror? -> t is DeclaredType }
                .map<DeclaredType> { t: TypeMirror? -> t as DeclaredType? }
                .filter(Predicate { t: DeclaredType -> types.isAssignable(t.asElement().asType(), redisElement.asType()) })
                .findFirst()
        } else Optional.empty()
    }

    private fun getCacheImplBase(cacheContract: TypeElement, cacheType: DeclaredType): TypeName {
        val impl: ClassName
        impl = if ((cacheType.asElement() as TypeElement).qualifiedName.contentEquals(CAFFEINE_CACHE.canonicalName)) {
            CAFFEINE_CACHE_IMPL
        } else if ((cacheType.asElement() as TypeElement).qualifiedName.contentEquals(REDIS_CACHE.canonicalName)) {
            REDIS_CACHE_IMPL
        } else {
            throw UnsupportedOperationException("Unknown implementation: " + cacheContract.qualifiedName)
        }

        return ParameterizedTypeName(
            false,
            types.getDeclaredType(
                elements.getTypeElement(impl.canonicalName()),
                cacheType.typeArguments[0],
                cacheType.typeArguments[1]
            )
        )
    }

    private fun getCacheMethodConfig(cacheConfig: KSType, cacheType: KSType): FunSpec {
        val configPath = cacheConfig.getAnnotationsByType(Cache::class).first().value
        val cacheContractName = ClassName.(cacheConfig)
        val methodName = "%sConfig".formatted(cacheContractName.simpleName)
        val extractorType: DeclaredType
        val returnType: TypeMirror
        if ((cacheType.asElement() as TypeElement).qualifiedName.contentEquals(CAFFEINE_CACHE.canonicalName)) {
            returnType = elements.getTypeElement(CAFFEINE_CACHE_CONFIG.canonicalName()).asType()
            extractorType = types.getDeclaredType(elements.getTypeElement(CLASS_CONFIG_EXTRACTOR.canonicalName()), returnType)
        } else if ((cacheType.asElement() as TypeElement).qualifiedName.contentEquals(REDIS_CACHE.canonicalName())) {
            returnType = elements.getTypeElement(REDIS_CACHE_CONFIG.canonicalName()).asType()
            extractorType = types.getDeclaredType(elements.getTypeElement(CLASS_CONFIG_EXTRACTOR.canonicalName()), returnType)
        } else {
            throw UnsupportedOperationException("Unknown implementation: " + cacheConfig.qualifiedName)
        }
        return MethodSpec.methodBuilder(methodName)
            .addAnnotation(
                AnnotationSpec.builder(Tag::class.java)
                    .addMember("value", cacheContractName.simpleName() + ".class")
                    .build()
            )
            .addModifiers(Modifier.DEFAULT, Modifier.PUBLIC)
            .addParameter(CLASS_CONFIG, "config")
            .addParameter(TypeName.get(extractorType), "extractor")
            .addStatement("return extractor.extract(config.getValue(\$S))", "cache.$configPath")
            .returns(TypeName.get(returnType))
            .build()
    }

    private fun getCacheImpl(cacheContract: KSType): ClassName {
        val cacheImplName: ClassName = ClassName.get(cacheContract)
        return ClassName.get(cacheImplName.packageName(), "$%sImpl".formatted(cacheImplName.simpleName()))
    }

    private fun getCacheMethodImpl(cacheContract: TypeElement, cacheType: DeclaredType): FunSpec {
        val cacheImplName: ClassName = getCacheImpl(cacheContract)
        val methodName = "%sImpl".formatted(cacheImplName.simpleName())
        return if ((cacheType.asElement() as TypeElement).qualifiedName.contentEquals(CAFFEINE_CACHE.canonicalName())) {
            MethodSpec.methodBuilder(methodName)
                .addModifiers(Modifier.DEFAULT, Modifier.PUBLIC)
                .addParameter(CAFFEINE_CACHE_CONFIG, "config")
                .addParameter(CAFFEINE_CACHE_FACTORY, "factory")
                .addParameter(CLASS_CACHE_TELEMETRY, "telemetry")
                .addStatement("return new \$L(config, factory, telemetry)", cacheImplName)
                .returns(TypeName.get(cacheContract.asType()))
                .build()
        } else if ((cacheType.asElement() as TypeElement).qualifiedName.contentEquals(REDIS_CACHE.canonicalName())) {
            val keyType = cacheType.typeArguments[0]
            val valueType = cacheType.typeArguments[1]
            val keyMapperType: DeclaredType = types.getDeclaredType(elements.getTypeElement(REDIS_CACHE_MAPPER_KEY.canonicalName()), keyType)
            val valueMapperType: DeclaredType = types.getDeclaredType(elements.getTypeElement(REDIS_CACHE_MAPPER_VALUE.canonicalName()), valueType)
            MethodSpec.methodBuilder(methodName)
                .addModifiers(Modifier.DEFAULT, Modifier.PUBLIC)
                .addParameter(REDIS_CACHE_CONFIG, "config")
                .addParameter(REDIS_CACHE_CLIENT_SYNC, "syncClient")
                .addParameter(REDIS_CACHE_CLIENT_REACTIVE, "reactiveClient")
                .addParameter(CLASS_CACHE_TELEMETRY, "telemetry")
                .addParameter(TypeName.get(keyMapperType), "keyMapper")
                .addParameter(TypeName.get(valueMapperType), "valueMapper")
                .addStatement("return new \$L(config, syncClient, reactiveClient, telemetry, keyMapper, valueMapper)", methodName)
                .returns(TypeName.get(cacheContract.asType()))
                .build()
        } else {
            throw UnsupportedOperationException("Unknown implementation: " + cacheContract.qualifiedName)
        }
    }

    private fun getCacheConstructor(cacheContract: KSType, cacheType: KSType): FunSpec {
        val cacheConfigName = cacheContract.getAnnotation(Cache::class.java).value
        require(NAME_PATTERN.matcher(cacheConfigName).find()) { "Cache config path doesn't match pattern: " + NAME_PATTERN }
        return if ((cacheType.asElement() as TypeElement).qualifiedName.contentEquals(CAFFEINE_CACHE.canonicalName)) {
            FunSpec.constructorBuilder()
                .addModifiers(Modifier.PUBLIC)
                .addParameter(CAFFEINE_CACHE_CONFIG, "config")
                .addParameter(CAFFEINE_CACHE_FACTORY, "factory")
                .addParameter(CLASS_CACHE_TELEMETRY, "telemetry")
                .addStatement("super(\$S, config, factory, telemetry)", cacheConfigName)
                .build()
        } else if ((cacheType.asElement() as TypeElement).qualifiedName.contentEquals(REDIS_CACHE.canonicalName)) {
            val keyType = (cacheContract.asType() as DeclaredType).typeArguments[0]
            val valueType = (cacheContract.asType() as DeclaredType).typeArguments[1]
            val keyMapperType: DeclaredType = types.getDeclaredType(elements.getTypeElement(REDIS_CACHE_MAPPER_KEY.canonicalName), keyType)
            val valueMapperType: DeclaredType = types.getDeclaredType(elements.getTypeElement(REDIS_CACHE_MAPPER_VALUE.canonicalName), valueType)
            FunSpec.constructorBuilder()
                .addModifiers(Modifier.PROTECTED)
                .addParameter(REDIS_CACHE_CONFIG, "config")
                .addParameter(REDIS_CACHE_CLIENT_SYNC, "syncClient")
                .addParameter(REDIS_CACHE_CLIENT_REACTIVE, "reactiveClient")
                .addParameter(CLASS_CACHE_TELEMETRY, "telemetry")
                .addParameter(TypeName.get(keyMapperType), "keyMapper")
                .addParameter(TypeName.get(valueMapperType), "valueMapper")
                .addStatement("super(\$S, config, syncClient, reactiveClient, telemetry, keyMapper, valueMapper)", cacheConfigName)
                .build()
        } else {
            throw UnsupportedOperationException("Unknown implementation: " + cacheContract.qualifiedName)
        }
    }

    private fun getPackage(element: KSAnnotated): String {
        return processingEnv.getElementUtils().getPackageOf(element).getQualifiedName().toString()
    }
}

