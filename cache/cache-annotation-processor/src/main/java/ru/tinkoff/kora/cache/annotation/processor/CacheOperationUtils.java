package ru.tinkoff.kora.cache.annotation.processor;

import ru.tinkoff.kora.annotation.processor.common.ProcessingError;
import ru.tinkoff.kora.annotation.processor.common.ProcessingErrorException;
import ru.tinkoff.kora.cache.annotation.*;

import javax.lang.model.element.*;
import javax.tools.Diagnostic;
import java.lang.annotation.Annotation;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.Set;

public final class CacheOperationUtils {

    private static final Set<Class<? extends Annotation>> CACHE_ANNOTATIONS = Set.of(
        Cacheable.class, Cacheables.class,
        CachePut.class, CachePuts.class,
        CacheInvalidate.class, CacheInvalidates.class);

    private CacheOperationUtils() {}

    public static CacheOperation getCacheMeta(ExecutableElement method) {
        final List<AnnotationMirror> cacheables = getRepeatedAnnotations(method, Cacheable.class, Cacheables.class);
        final List<AnnotationMirror> puts = getRepeatedAnnotations(method, CachePut.class, CachePuts.class);
        final List<AnnotationMirror> invalidates = getRepeatedAnnotations(method, CacheInvalidate.class, CacheInvalidates.class);

        final String className = method.getEnclosingElement().getSimpleName().toString();
        final String methodName = method.getSimpleName().toString();
        final CacheOperation.Origin origin = new CacheOperation.Origin(className, methodName);

        if (!cacheables.isEmpty()) {
            if (!puts.isEmpty() || !invalidates.isEmpty()) {
                throw new ProcessingErrorException(new ProcessingError(Diagnostic.Kind.ERROR,
                    "Method must have Cache annotations with same operation type, but got multiple different operation types for " + origin, method));
            }

            return getOperation(method, cacheables, CacheOperation.Type.GET);
        } else if (!puts.isEmpty()) {
            if (!invalidates.isEmpty()) {
                throw new ProcessingErrorException(new ProcessingError(Diagnostic.Kind.ERROR,
                    "Method must have Cache annotations with same operation type, but got multiple different operation types for " + origin, method));
            }

            return getOperation(method, puts, CacheOperation.Type.PUT);
        } else if (!invalidates.isEmpty()) {
            var invalidateAlls = invalidates.stream()
                .flatMap(a -> a.getElementValues().entrySet().stream())
                .filter(e -> e.getKey().getSimpleName().contentEquals("invalidateAll"))
                .map(e -> ((boolean) e.getValue().getValue()))
                .toList();

            final boolean anyInvalidateAll = !invalidateAlls.isEmpty() && invalidateAlls.stream().anyMatch(v -> v);
            final boolean allInvalidateAll = !invalidateAlls.isEmpty() && invalidateAlls.stream().allMatch(v -> v);

            if (anyInvalidateAll && !allInvalidateAll) {
                throw new ProcessingErrorException(new ProcessingError(Diagnostic.Kind.ERROR,
                    CacheInvalidate.class + " not all annotations are marked 'invalidateAll' out of all for " + origin, method));
            }

            final CacheOperation.Type type = (allInvalidateAll) ? CacheOperation.Type.EVICT_ALL : CacheOperation.Type.EVICT;
            return getOperation(method, invalidates, type);
        }

        throw new ProcessingErrorException(new ProcessingError(Diagnostic.Kind.ERROR,
            "None of " + CACHE_ANNOTATIONS + " cache annotations found", method));
    }

    private static CacheOperation getOperation(ExecutableElement method, List<AnnotationMirror> cacheAnnotations, CacheOperation.Type type) {
        final String className = method.getEnclosingElement().getSimpleName().toString();
        final String methodName = method.getSimpleName().toString();
        final CacheOperation.Origin origin = new CacheOperation.Origin(className, methodName);

        final List<List<String>> cacheKeyArguments = new ArrayList<>();
        final List<String> cacheImpls = new ArrayList<>();
        for (var annotation : cacheAnnotations) {
            var parameters = annotation.getElementValues().entrySet().stream()
                .filter(e -> e.getKey().getSimpleName().contentEquals("parameters"))
                .map(e -> ((List<?>) (e.getValue()).getValue()).stream()
                    .filter(a -> a instanceof AnnotationValue)
                    .map(a -> ((AnnotationValue) a).getValue().toString())
                    .toList())
                .findFirst()
                .orElse(Collections.emptyList());

            if (parameters.isEmpty()) {
                parameters = method.getParameters().stream()
                    .map(p -> p.getSimpleName().toString())
                    .toList();
            } else {
                for (String parameter : parameters) {
                    if (method.getParameters().stream().noneMatch(p -> p.getSimpleName().contentEquals(parameter))) {
                        throw new ProcessingErrorException(new ProcessingError(Diagnostic.Kind.ERROR,
                            "Unknown method parameter is declared: " + parameter, method));
                    }
                }
            }

            for (List<String> arguments : cacheKeyArguments) {
                if (!arguments.equals(parameters)) {
                    throw new ProcessingErrorException(new ProcessingError(Diagnostic.Kind.ERROR,
                        annotation.getClass() + " parameters mismatch for different annotations for: " + origin, method));
                }
            }

            cacheKeyArguments.add(parameters);

            final String cacheImpl = annotation.getElementValues().entrySet().stream()
                .filter(e -> e.getKey().getSimpleName().contentEquals("value"))
                .map(e -> String.valueOf(e.getValue().getValue()))
                .findFirst()
                .orElseThrow();
            cacheImpls.add(cacheImpl);
        }

        final List<VariableElement> parameterResult = cacheKeyArguments.get(0).stream()
            .flatMap(param -> method.getParameters().stream().filter(p -> p.getSimpleName().contentEquals(param)))
            .map(p -> ((VariableElement) p))
            .toList();

        return new CacheOperation(type, cacheImpls, parameterResult, origin);
    }

    private static List<AnnotationMirror> getRepeatedAnnotations(Element element,
                                                                 Class<? extends Annotation> annotation,
                                                                 Class<? extends Annotation> parentAnnotation) {
        return getRepeatedAnnotations(element, annotation.getCanonicalName(), parentAnnotation.getCanonicalName());
    }

    private static List<AnnotationMirror> getRepeatedAnnotations(Element element,
                                                                 String annotation,
                                                                 String parentAnnotation) {
        final List<AnnotationMirror> repeated = element.getAnnotationMirrors().stream()
            .filter(pa -> pa.getAnnotationType().toString().contentEquals(parentAnnotation))
            .flatMap(pa -> pa.getElementValues().entrySet().stream())
            .flatMap(e -> ((List<?>) e.getValue().getValue()).stream().map(AnnotationMirror.class::cast))
            .filter(a -> a.getAnnotationType().toString().contentEquals(annotation))
            .toList();

        if (!repeated.isEmpty()) {
            return repeated;
        }

        return element.getAnnotationMirrors().stream()
            .filter(a -> a.getAnnotationType().toString().contentEquals(annotation))
            .map(a -> ((AnnotationMirror) a))
            .toList();
    }
}
