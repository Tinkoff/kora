package ru.tinkoff.kora.annotation.processor.common;

import reactor.core.publisher.Flux;
import reactor.core.publisher.Mono;

import javax.annotation.processing.ProcessingEnvironment;
import javax.lang.model.element.ExecutableElement;
import javax.lang.model.type.DeclaredType;
import javax.lang.model.type.TypeKind;
import javax.lang.model.type.TypeMirror;
import javax.lang.model.util.Types;
import java.util.Optional;
import java.util.concurrent.Future;

public final class MethodUtils {

    private MethodUtils() {}

    public static boolean isMono(ExecutableElement method, ProcessingEnvironment env) {
        return isMono(method, env.getTypeUtils());
    }

    public static boolean isMono(ExecutableElement method, Types types) {
        return isMono(method.getReturnType(), types);
    }

    public static boolean isMono(TypeMirror method, Types types) {
        final TypeMirror returnType = types.erasure(method);
        return types.isAssignable(returnType, returnType);
    }

    public static boolean isMono(TypeMirror returnType) {
        return returnType.toString().startsWith(Mono.class.getCanonicalName());
    }

    public static boolean isFuture(ExecutableElement method, ProcessingEnvironment env) {
        return isFuture(method.getReturnType(), env);
    }

    public static boolean isFuture(TypeMirror returnType, ProcessingEnvironment env) {
        var targetType = env.getElementUtils().getTypeElement(Future.class.getCanonicalName());
        return env.getTypeUtils().isAssignable(returnType, targetType.asType());
    }

    public static boolean isFlux(ExecutableElement method, ProcessingEnvironment env) {
        final TypeMirror returnType = env.getTypeUtils().erasure(method.getReturnType());
        return returnType.toString().equals(Flux.class.getCanonicalName());
    }

    public static boolean isFlux(TypeMirror returnType) {
        return returnType.toString().equals(Flux.class.getCanonicalName());
    }

    public static boolean isVoid(ExecutableElement method) {
        final TypeMirror returnType = method.getReturnType();
        return isVoid(returnType);
    }

    public static boolean isVoid(TypeMirror returnType) {
        final String typeAsStr = returnType.toString();
        return returnType.getKind().equals(TypeKind.VOID)
            || Void.class.getCanonicalName().equals(typeAsStr)
            || "void".equals(typeAsStr);
    }

    public static Optional<TypeMirror> getGenericType(TypeMirror typeMirror) {
        if (typeMirror instanceof DeclaredType) {
            return ((DeclaredType) typeMirror).getTypeArguments().stream()
                .map(v -> ((TypeMirror) v))
                .findFirst();
        } else {
            return Optional.empty();
        }
    }
}
