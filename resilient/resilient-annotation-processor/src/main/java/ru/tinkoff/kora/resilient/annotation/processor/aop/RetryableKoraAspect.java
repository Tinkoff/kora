package ru.tinkoff.kora.resilient.annotation.processor.aop;

import com.squareup.javapoet.CodeBlock;
import ru.tinkoff.kora.annotation.processor.common.MethodUtils;
import ru.tinkoff.kora.annotation.processor.common.ProcessingError;
import ru.tinkoff.kora.annotation.processor.common.ProcessingErrorException;
import ru.tinkoff.kora.aop.annotation.processor.KoraAspect;

import javax.annotation.processing.ProcessingEnvironment;
import javax.lang.model.element.AnnotationMirror;
import javax.lang.model.element.ExecutableElement;
import javax.tools.Diagnostic;
import java.util.List;
import java.util.Optional;
import java.util.Set;
import java.util.concurrent.Future;

import static com.squareup.javapoet.CodeBlock.joining;

public class RetryableKoraAspect implements KoraAspect {

    private static final String ANNOTATION_TYPE = "ru.tinkoff.kora.resilient.retry.annotation.Retryable";

    private final ProcessingEnvironment env;

    public RetryableKoraAspect(ProcessingEnvironment env) {
        this.env = env;
    }

    @Override
    public Set<String> getSupportedAnnotationTypes() {
        return Set.of(ANNOTATION_TYPE);
    }

    @Override
    public ApplyResult apply(ExecutableElement method, String superCall, AspectContext aspectContext) {
        if (MethodUtils.isFuture(method, env)) {
            throw new ProcessingErrorException(new ProcessingError(Diagnostic.Kind.NOTE, "@Retryable can't be applied for types assignable from " + Future.class, method));
        }

        final Optional<? extends AnnotationMirror> mirror = method.getAnnotationMirrors().stream().filter(a -> a.getAnnotationType().toString().equals(ANNOTATION_TYPE)).findFirst();
        final String retryableName = mirror.flatMap(a -> a.getElementValues().entrySet().stream()
                .filter(e -> e.getKey().getSimpleName().contentEquals("value"))
                .map(e -> String.valueOf(e.getValue().getValue())).findFirst())
            .orElseThrow();

        var managerType = env.getTypeUtils().getDeclaredType(env.getElementUtils().getTypeElement("ru.tinkoff.kora.resilient.retry.RetrierManager"));
        var fieldManager = aspectContext.fieldFactory().constructorParam(managerType, List.of());

        final CodeBlock body;
        if (MethodUtils.isMono(method, env)) {
            body = buildBodyMono(method, superCall, retryableName, fieldManager);
        } else if (MethodUtils.isFlux(method, env)) {
            body = buildBodyFlux(method, superCall, retryableName, fieldManager);
        } else {
            body = buildBodySync(method, superCall, retryableName, fieldManager);
        }

        return new ApplyResult.MethodBody(body);
    }

    private CodeBlock buildBodySync(ExecutableElement method, String superCall, String retryableName, String fieldManager) {
        final CodeBlock retrierExecution = CodeBlock.of("retry($L)", buildMethodCallable(method, superCall));
        final String returnPrefix = MethodUtils.isVoid(method)
            ? ""
            : "return ";

        return CodeBlock.builder().add("""
            var _retrier = $L.get("$L");
            $L_retrier.$L;
            """, fieldManager, retryableName, returnPrefix, retrierExecution).build();
    }

    private CodeBlock buildBodyMono(ExecutableElement method, String superCall, String retryableName, String fieldManager) {
        return CodeBlock.builder().add("""
            var _retrier = $L.get("$L");
            return $L.retryWhen(_retrier.asReactor());
            """, fieldManager, retryableName, buildMethodCall(method, superCall)).build();
    }

    private CodeBlock buildBodyFlux(ExecutableElement method, String superCall, String retryableName, String fieldManager) {
        return CodeBlock.builder().add("""
            var _retrier = $L.get("$L");
            return $L.retryWhen(_retrier.asReactor());
            """, fieldManager, retryableName, buildMethodCall(method, superCall)).build();
    }

    private CodeBlock buildMethodCall(ExecutableElement method, String call) {
        return method.getParameters().stream().map(p -> CodeBlock.of("$L", p)).collect(joining(", ", call + "(", ")"));
    }

    private CodeBlock buildMethodCallable(ExecutableElement method, String call) {
        return method.getParameters().stream().map(p -> CodeBlock.of("$L", p)).collect(joining(", ", "() -> " + call + "(", ")"));
    }
}
