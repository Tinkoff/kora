package ru.tinkoff.kora.cache.annotation.processor.aop;

import com.squareup.javapoet.AnnotationSpec;
import com.squareup.javapoet.ClassName;
import com.squareup.javapoet.CodeBlock;
import ru.tinkoff.kora.aop.annotation.processor.KoraAspect;
import ru.tinkoff.kora.cache.annotation.processor.CacheOperation;
import ru.tinkoff.kora.common.Tag;

import javax.annotation.processing.ProcessingEnvironment;
import javax.lang.model.element.ExecutableElement;
import javax.lang.model.element.TypeElement;

import java.util.ArrayList;
import java.util.List;
import java.util.stream.Collectors;

import static com.squareup.javapoet.CodeBlock.joining;

abstract class AbstractAopCacheAspect implements KoraAspect {

    private static final ClassName KEY_CACHE = ClassName.get("ru.tinkoff.kora.cache", "CacheKey");
    private static final ClassName KEY_CACHE2 = ClassName.get("ru.tinkoff.kora.cache", "CacheKey", "Key2");
    private static final ClassName KEY_CACHE3 = ClassName.get("ru.tinkoff.kora.cache", "CacheKey", "Key3");
    private static final ClassName KEY_CACHE4 = ClassName.get("ru.tinkoff.kora.cache", "CacheKey", "Key4");
    private static final ClassName KEY_CACHE5 = ClassName.get("ru.tinkoff.kora.cache", "CacheKey", "Key5");
    private static final ClassName KEY_CACHE6 = ClassName.get("ru.tinkoff.kora.cache", "CacheKey", "Key6");
    private static final ClassName KEY_CACHE7 = ClassName.get("ru.tinkoff.kora.cache", "CacheKey", "Key7");
    private static final ClassName KEY_CACHE8 = ClassName.get("ru.tinkoff.kora.cache", "CacheKey", "Key8");
    private static final ClassName KEY_CACHE9 = ClassName.get("ru.tinkoff.kora.cache", "CacheKey", "Key9");

    ClassName getCacheKey(CacheOperation meta) {
        return KEY_CACHE;
//
//        return switch (meta.parameters().size()) {
//            case 2 -> KEY_CACHE2;
//            case 3 -> KEY_CACHE3;
//            case 4 -> KEY_CACHE4;
//            case 5 -> KEY_CACHE5;
//            case 6 -> KEY_CACHE6;
//            case 7 -> KEY_CACHE7;
//            case 8 -> KEY_CACHE8;
//            case 9 -> KEY_CACHE9;
//            default -> throw new IllegalArgumentException("Can't provide CacheKey for '" +  meta.parameters().size() + "' arguments");
//        };
    }

    List<String> getCacheFields(CacheOperation operation, ProcessingEnvironment env, AspectContext aspectContext) {
        final List<String> cacheFields = new ArrayList<>();
        for (var cacheImpl: operation.cacheImplementations()) {
            var cacheElement = env.getElementUtils().getTypeElement(cacheImpl);
            var fieldCache = aspectContext.fieldFactory().constructorParam(cacheElement.asType(), List.of());
            cacheFields.add(fieldCache);
        }

        return cacheFields;
    }

    String getKeyRecordParameters(CacheOperation operation, ExecutableElement method) {
        return String.join(", ", operation.getParametersNames(method));
    }

    String getSuperMethod(ExecutableElement method, String superCall) {
        return method.getParameters().stream()
            .map(p -> CodeBlock.of("$L", p))
            .collect(joining(", ", superCall + "(", ")")).toString();
    }
}
