package ru.tinkoff.kora.cache.annotation.processor;

import com.squareup.javapoet.*;
import ru.tinkoff.kora.annotation.processor.common.AbstractKoraProcessor;
import ru.tinkoff.kora.annotation.processor.common.CommonClassNames;
import ru.tinkoff.kora.cache.annotation.Cache;
import ru.tinkoff.kora.common.Module;
import ru.tinkoff.kora.common.Tag;

import javax.annotation.processing.ProcessingEnvironment;
import javax.annotation.processing.RoundEnvironment;
import javax.lang.model.element.Element;
import javax.lang.model.element.Modifier;
import javax.lang.model.element.TypeElement;
import javax.lang.model.type.DeclaredType;
import javax.lang.model.type.TypeMirror;
import javax.tools.Diagnostic;
import java.io.IOException;
import java.lang.annotation.Annotation;
import java.util.List;
import java.util.Optional;
import java.util.Set;
import java.util.regex.Pattern;
import java.util.stream.Collectors;

public class CacheAnnotationProcessor extends AbstractKoraProcessor {

    private static final Pattern NAME_PATTERN = Pattern.compile("^[a-zA-Z][0-9a-zA-Z_]*");

    private static final ClassName ANNOTATION_CACHE = ClassName.get("ru.tinkoff.kora.cache.annotation", "Cache");

    private static final ClassName CLASS_CACHE = ClassName.get("ru.tinkoff.kora.cache", "Cache");
    private static final ClassName CLASS_CACHE_TELEMETRY = ClassName.get("ru.tinkoff.kora.cache.telemetry", "CacheTelemetry");
    private static final ClassName CLASS_CONFIG = ClassName.get("com.typesafe.config", "Config");
    private static final ClassName CLASS_CONFIG_EXTRACTOR = ClassName.get("ru.tinkoff.kora.config.common.extractor", "ConfigValueExtractor");

    private static final ClassName CAFFEINE_CACHE = ClassName.get("ru.tinkoff.kora.cache.caffeine", "CaffeineCache");
    private static final ClassName CAFFEINE_CACHE_FACTORY = ClassName.get("ru.tinkoff.kora.cache.caffeine", "CaffeineCacheFactory");
    private static final ClassName CAFFEINE_CACHE_CONFIG = ClassName.get("ru.tinkoff.kora.cache.caffeine", "CaffeineCacheConfig");
    private static final ClassName CAFFEINE_CACHE_IMPL = ClassName.get("ru.tinkoff.kora.cache.caffeine", "AbstractCaffeineCache");

    private static final ClassName REDIS_CACHE = ClassName.get("ru.tinkoff.kora.cache.redis", "RedisCache");
    private static final ClassName REDIS_CACHE_IMPL = ClassName.get("ru.tinkoff.kora.cache.redis", "AbstractRedisCache");
    private static final ClassName REDIS_CACHE_CONFIG = ClassName.get("ru.tinkoff.kora.cache.redis", "RedisCacheConfig");
    private static final ClassName REDIS_CACHE_CLIENT_SYNC = ClassName.get("ru.tinkoff.kora.cache.redis", "SyncRedisClient");
    private static final ClassName REDIS_CACHE_CLIENT_REACTIVE = ClassName.get("ru.tinkoff.kora.cache.redis", "ReactiveRedisClient");
    private static final ClassName REDIS_CACHE_MAPPER_KEY = ClassName.get("ru.tinkoff.kora.cache.redis", "RedisCacheKeyMapper");
    private static final ClassName REDIS_CACHE_MAPPER_VALUE = ClassName.get("ru.tinkoff.kora.cache.redis", "RedisCacheValueMapper");

    private static Set<Class<? extends Annotation>> getSupportedAnnotations() {
        return Set.of(Cache.class);
    }

    @Override
    public synchronized void init(ProcessingEnvironment processingEnv) {
        super.init(processingEnv);
    }

    @Override
    public Set<String> getSupportedAnnotationTypes() {
        return getSupportedAnnotations().stream()
            .map(Class::getCanonicalName)
            .collect(Collectors.toSet());
    }

    @Override
    public boolean process(Set<? extends TypeElement> annotations, RoundEnvironment roundEnv) {
        final List<TypeElement> annotatedElements = getAnnotatedElements(roundEnv);
        for (TypeElement cacheContract : annotatedElements) {
            try {
                if (!cacheContract.getKind().isInterface()) {
                    throw new IllegalArgumentException("@Cache annotation is intended to be used on interfaces, but was: " + cacheContract.getKind().name());
                }

                final Optional<DeclaredType> cacheContractType = getCacheSuperType(cacheContract);
                if (cacheContractType.isEmpty()) {
                    throw new IllegalArgumentException("@Cache is expected to be known super type "
                                                       + CAFFEINE_CACHE.canonicalName()
                                                       + " or "
                                                       + REDIS_CACHE.canonicalName()
                                                       + ", but was: " + cacheContract.getSuperclass());
                }

                final String packageName = getPackage(cacheContract);
                final ClassName cacheImplName = ClassName.get(cacheContract);

                var cacheImplBase = getCacheImplBase(cacheContract, cacheContractType.get());
                var implSpec = TypeSpec.classBuilder(getCacheImpl(cacheContract))
                    .addModifiers(Modifier.FINAL)
                    .addAnnotation(AnnotationSpec.builder(CommonClassNames.koraGenerated)
                        .addMember("value", CodeBlock.of("$S", CacheAnnotationProcessor.class.getCanonicalName())).build())
                    .addMethod(getCacheConstructor(cacheContract, cacheContractType.get()))
                    .superclass(cacheImplBase)
                    .addSuperinterface(cacheContract.asType())
                    .build();

                final JavaFile implFile = JavaFile.builder(cacheImplName.packageName(), implSpec).build();
                implFile.writeTo(processingEnv.getFiler());

                var moduleSpec = TypeSpec.interfaceBuilder(ClassName.get(packageName, "$%sModule".formatted(cacheImplName.simpleName())))
                    .addModifiers(Modifier.PUBLIC)
                    .addAnnotation(AnnotationSpec.builder(CommonClassNames.koraGenerated)
                        .addMember("value", CodeBlock.of("$S", CacheAnnotationProcessor.class.getCanonicalName())).build())
                    .addAnnotation(Module.class)
                    .addMethod(getCacheMethodImpl(cacheContract, cacheContractType.get()))
                    .addMethod(getCacheMethodConfig(cacheContract, cacheContractType.get()))
                    .build();

                final JavaFile moduleFile = JavaFile.builder(cacheImplName.packageName(), moduleSpec).build();
                moduleFile.writeTo(processingEnv.getFiler());
            } catch (IOException e) {
                processingEnv.getMessager().printMessage(Diagnostic.Kind.ERROR, e.getMessage(), cacheContract);
                e.printStackTrace();
                return false;
            } catch (IllegalArgumentException | IllegalStateException e) {
                processingEnv.getMessager().printMessage(Diagnostic.Kind.ERROR, e.getMessage(), cacheContract);
                return false;
            }
        }

        return false;
    }

    private Optional<DeclaredType> getCacheSuperType(TypeElement candidate) {
        final TypeElement caffeineElement = elements.getTypeElement(CAFFEINE_CACHE.canonicalName());
        if (caffeineElement != null) {
            return types.directSupertypes(candidate.asType()).stream()
                .filter(t -> t instanceof DeclaredType)
                .map(t -> ((DeclaredType) t))
                .filter(t -> types.isAssignable(t.asElement().asType(), caffeineElement.asType()))
                .findFirst();
        }

        final TypeElement redisElement = elements.getTypeElement(REDIS_CACHE.canonicalName());
        if (redisElement != null) {
            return types.directSupertypes(candidate.asType()).stream()
                .filter(t -> t instanceof DeclaredType)
                .map(t -> ((DeclaredType) t))
                .filter(t -> types.isAssignable(t.asElement().asType(), redisElement.asType()))
                .findFirst();
        }

        return Optional.empty();
    }

    private TypeName getCacheImplBase(TypeElement cacheContract, DeclaredType cacheType) {
        final ClassName impl;
        if (((TypeElement) cacheType.asElement()).getQualifiedName().contentEquals(CAFFEINE_CACHE.canonicalName())) {
            impl = CAFFEINE_CACHE_IMPL;
        } else if (((TypeElement) cacheType.asElement()).getQualifiedName().contentEquals(REDIS_CACHE.canonicalName())) {
            impl = REDIS_CACHE_IMPL;
        } else {
            throw new UnsupportedOperationException("Unknown implementation: " + cacheContract.getQualifiedName());
        }

        return TypeName.get(types.getDeclaredType(elements.getTypeElement(impl.canonicalName()),
            cacheType.getTypeArguments().get(0),
            cacheType.getTypeArguments().get(1)));
    }

    private MethodSpec getCacheMethodConfig(TypeElement cacheConfig, DeclaredType cacheType) {
        final String configPath = cacheConfig.getAnnotation(Cache.class).value();
        final ClassName cacheContractName = ClassName.get(cacheConfig);
        final String methodName = "%sConfig".formatted(cacheContractName.simpleName());
        final DeclaredType extractorType;
        final TypeMirror returnType;
        if (((TypeElement) cacheType.asElement()).getQualifiedName().contentEquals(CAFFEINE_CACHE.canonicalName())) {
            returnType = elements.getTypeElement(CAFFEINE_CACHE_CONFIG.canonicalName()).asType();
            extractorType = types.getDeclaredType(elements.getTypeElement(CLASS_CONFIG_EXTRACTOR.canonicalName()), returnType);
        } else if (((TypeElement) cacheType.asElement()).getQualifiedName().contentEquals(REDIS_CACHE.canonicalName())) {
            returnType = elements.getTypeElement(REDIS_CACHE_CONFIG.canonicalName()).asType();
            extractorType = types.getDeclaredType(elements.getTypeElement(CLASS_CONFIG_EXTRACTOR.canonicalName()), returnType);
        } else {
            throw new UnsupportedOperationException("Unknown implementation: " + cacheConfig.getQualifiedName());
        }

        return MethodSpec.methodBuilder(methodName)
            .addAnnotation(AnnotationSpec.builder(Tag.class)
                .addMember("value", cacheContractName.simpleName() + ".class")
                .build())
            .addModifiers(Modifier.DEFAULT, Modifier.PUBLIC)
            .addParameter(CLASS_CONFIG, "config")
            .addParameter(TypeName.get(extractorType), "extractor")
            .addStatement("return extractor.extract(config.getValue($S))", configPath)
            .returns(TypeName.get(returnType))
            .build();
    }

    private static ClassName getCacheImpl(TypeElement cacheContract) {
        final ClassName cacheImplName = ClassName.get(cacheContract);
        return ClassName.get(cacheImplName.packageName(), "$%sImpl".formatted(cacheImplName.simpleName()));
    }

    private MethodSpec getCacheMethodImpl(TypeElement cacheContract, DeclaredType cacheType) {
        final ClassName cacheImplName = getCacheImpl(cacheContract);
        final String methodName = "%sImpl".formatted(cacheImplName.simpleName());
        if (((TypeElement) cacheType.asElement()).getQualifiedName().contentEquals(CAFFEINE_CACHE.canonicalName())) {
            return MethodSpec.methodBuilder(methodName)
                .addModifiers(Modifier.DEFAULT, Modifier.PUBLIC)
                .addParameter(ParameterSpec.builder(CAFFEINE_CACHE_CONFIG, "config")
                    .addAnnotation(AnnotationSpec.builder(Tag.class)
                        .addMember("value", cacheContract.getSimpleName().toString() + ".class")
                        .build())
                    .build())
                .addParameter(CAFFEINE_CACHE_FACTORY, "factory")
                .addParameter(CLASS_CACHE_TELEMETRY, "telemetry")
                .addStatement("return new $L(config, factory, telemetry)", cacheImplName)
                .returns(TypeName.get(cacheContract.asType()))
                .build();
        } else if (((TypeElement) cacheType.asElement()).getQualifiedName().contentEquals(REDIS_CACHE.canonicalName())) {
            final TypeMirror keyType = cacheType.getTypeArguments().get(0);
            final TypeMirror valueType = cacheType.getTypeArguments().get(1);
            final DeclaredType keyMapperType = types.getDeclaredType(elements.getTypeElement(REDIS_CACHE_MAPPER_KEY.canonicalName()), keyType);
            final DeclaredType valueMapperType = types.getDeclaredType(elements.getTypeElement(REDIS_CACHE_MAPPER_VALUE.canonicalName()), valueType);
            return MethodSpec.methodBuilder(methodName)
                .addModifiers(Modifier.DEFAULT, Modifier.PUBLIC)
                .addParameter(ParameterSpec.builder(REDIS_CACHE_CONFIG, "config")
                    .addAnnotation(AnnotationSpec.builder(Tag.class)
                        .addMember("value", cacheContract.getSimpleName().toString() + ".class")
                        .build())
                    .build())
                .addParameter(REDIS_CACHE_CLIENT_SYNC, "syncClient")
                .addParameter(REDIS_CACHE_CLIENT_REACTIVE, "reactiveClient")
                .addParameter(CLASS_CACHE_TELEMETRY, "telemetry")
                .addParameter(TypeName.get(keyMapperType), "keyMapper")
                .addParameter(TypeName.get(valueMapperType), "valueMapper")
                .addStatement("return new $L(config, syncClient, reactiveClient, telemetry, keyMapper, valueMapper)", methodName)
                .returns(TypeName.get(cacheContract.asType()))
                .build();
        } else {
            throw new UnsupportedOperationException("Unknown implementation: " + cacheContract.getQualifiedName());
        }
    }

    private MethodSpec getCacheConstructor(TypeElement cacheContract, DeclaredType cacheType) {
        final String cacheConfigName = cacheContract.getAnnotation(Cache.class).value();
        if (!NAME_PATTERN.matcher(cacheConfigName).find()) {
            throw new IllegalArgumentException("Cache config path doesn't match pattern: " + NAME_PATTERN);
        }

        if (((TypeElement) cacheType.asElement()).getQualifiedName().contentEquals(CAFFEINE_CACHE.canonicalName())) {
            return MethodSpec.constructorBuilder()
                .addModifiers(Modifier.PUBLIC)
                .addParameter(CAFFEINE_CACHE_CONFIG, "config")
                .addParameter(CAFFEINE_CACHE_FACTORY, "factory")
                .addParameter(CLASS_CACHE_TELEMETRY, "telemetry")
                .addStatement("super($S, config, factory, telemetry)", cacheConfigName)
                .build();
        } else if (((TypeElement) cacheType.asElement()).getQualifiedName().contentEquals(REDIS_CACHE.canonicalName())) {
            final TypeMirror keyType = ((DeclaredType) cacheContract.asType()).getTypeArguments().get(0);
            final TypeMirror valueType = ((DeclaredType) cacheContract.asType()).getTypeArguments().get(1);
            final DeclaredType keyMapperType = types.getDeclaredType(elements.getTypeElement(REDIS_CACHE_MAPPER_KEY.canonicalName()), keyType);
            final DeclaredType valueMapperType = types.getDeclaredType(elements.getTypeElement(REDIS_CACHE_MAPPER_VALUE.canonicalName()), valueType);
            return MethodSpec.constructorBuilder()
                .addModifiers(Modifier.PROTECTED)
                .addParameter(REDIS_CACHE_CONFIG, "config")
                .addParameter(REDIS_CACHE_CLIENT_SYNC, "syncClient")
                .addParameter(REDIS_CACHE_CLIENT_REACTIVE, "reactiveClient")
                .addParameter(CLASS_CACHE_TELEMETRY, "telemetry")
                .addParameter(TypeName.get(keyMapperType), "keyMapper")
                .addParameter(TypeName.get(valueMapperType), "valueMapper")
                .addStatement("super($S, config, syncClient, reactiveClient, telemetry, keyMapper, valueMapper)", cacheConfigName)
                .build();
        } else {
            throw new UnsupportedOperationException("Unknown implementation: " + cacheContract.getQualifiedName());
        }
    }

    private static List<TypeElement> getAnnotatedElements(RoundEnvironment roundEnv) {
        return getSupportedAnnotations().stream()
            .flatMap(a -> roundEnv.getElementsAnnotatedWith(a).stream())
            .filter(a -> a instanceof TypeElement)
            .map(a -> ((TypeElement) a))
            .toList();
    }

    private String getPackage(Element element) {
        return processingEnv.getElementUtils().getPackageOf(element).getQualifiedName().toString();
    }
}
