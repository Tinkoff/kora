package ru.tinkoff.kora.resilient.annotation.processor.aop;

import com.squareup.javapoet.ClassName;
import com.squareup.javapoet.CodeBlock;
import ru.tinkoff.kora.annotation.processor.common.MethodUtils;
import ru.tinkoff.kora.annotation.processor.common.ProcessingErrorException;
import ru.tinkoff.kora.aop.annotation.processor.KoraAspect;

import javax.annotation.processing.ProcessingEnvironment;
import javax.lang.model.element.AnnotationMirror;
import javax.lang.model.element.ExecutableElement;
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
            throw new ProcessingErrorException("@Retryable can't be applied for types assignable from " + Future.class, method);
        }

        final Optional<? extends AnnotationMirror> mirror = method.getAnnotationMirrors().stream().filter(a -> a.getAnnotationType().toString().equals(ANNOTATION_TYPE)).findFirst();
        final String retryableName = mirror.flatMap(a -> a.getElementValues().entrySet().stream()
                .filter(e -> e.getKey().getSimpleName().contentEquals("value"))
                .map(e -> String.valueOf(e.getValue().getValue())).findFirst())
            .orElseThrow();

        var managerType = env.getTypeUtils().getDeclaredType(env.getElementUtils().getTypeElement("ru.tinkoff.kora.resilient.retry.RetrierManager"));
        var fieldManager = aspectContext.fieldFactory().constructorParam(managerType, List.of());
        var retrierType = env.getTypeUtils().getDeclaredType(env.getElementUtils().getTypeElement("ru.tinkoff.kora.resilient.retry.Retrier"));
        var fieldRetrier = aspectContext.fieldFactory().constructorInitialized(retrierType,
            CodeBlock.of("$L.get($S);", fieldManager, retryableName));

        final CodeBlock body;
        if (MethodUtils.isMono(method, env)) {
            body = buildBodyMono(method, superCall, fieldRetrier);
        } else if (MethodUtils.isFlux(method, env)) {
            body = buildBodyFlux(method, superCall, fieldRetrier);
        } else {
            body = buildBodySync(method, superCall, fieldRetrier);
        }

        return new ApplyResult.MethodBody(body);
    }

    private static final ClassName CanRetryResult = ClassName.get("ru.tinkoff.kora.resilient.retry", "Retrier", "RetryState", "CanRetryResult");
    private static final ClassName CanRetry = CanRetryResult.nestedClass("CanRetry");
    private static final ClassName CantRetry = CanRetryResult.nestedClass("CantRetry");
    private static final ClassName RetryExhausted = CanRetryResult.nestedClass("RetryExhausted");


    private CodeBlock buildBodySync(ExecutableElement method, String superCall, String fieldRetry) {
        var b = CodeBlock.builder()
            .addStatement("var _cause = (Exception) null")
            .beginControlFlow("try (var _retryState = $L.asState())", fieldRetry)
            .beginControlFlow("while (true)")
            .beginControlFlow("try");
        if (MethodUtils.isVoid(method)) {
            b.addStatement(buildMethodCall(method, superCall));
            b.addStatement("return");
        } else {
            b.add("return ").addStatement(buildMethodCall(method, superCall));
        }
        b.nextControlFlow("catch (Exception _e)");
        b.addStatement("var _retry = _retryState.canRetry(_e)");
        b.beginControlFlow("if (_retry instanceof $T)", CantRetry)
            .addStatement("if (_cause != null) _e.addSuppressed(_cause)")
            .addStatement("throw _e")
            .endControlFlow();
        b.beginControlFlow("if (_retry instanceof $T _exhausted)", RetryExhausted)
            .addStatement("var _exhaustedException = _exhausted.toException()")
            .addStatement("if (_cause != null) _exhaustedException.addSuppressed(_cause)")
            .addStatement("throw _exhaustedException")
            .endControlFlow();
        b.beginControlFlow("if (_retry instanceof $T)", CanRetry)
            .beginControlFlow("if (_cause == null)")
            .addStatement("_cause = _e")
            .nextControlFlow("else")
            .addStatement("_cause.addSuppressed(_e)")
            .endControlFlow() //if (_cause == null)
            .addStatement("_retryState.doDelay()")
            .endControlFlow();

        b
            .endControlFlow() // try
            .endControlFlow() // while
            .endControlFlow() // try retry state
        ;
        return b.build();
    }

    private CodeBlock buildBodyMono(ExecutableElement method, String superCall, String fieldRetry) {
        return CodeBlock.builder().add("""
            return $L.retryWhen($L.asReactor());
            """, buildMethodCall(method, superCall), fieldRetry).build();
    }

    private CodeBlock buildBodyFlux(ExecutableElement method, String superCall, String fieldRetry) {
        return CodeBlock.builder().add("""
            return $L.retryWhen($L.asReactor());
            """, buildMethodCall(method, superCall), fieldRetry).build();
    }

    private CodeBlock buildMethodCall(ExecutableElement method, String call) {
        return method.getParameters().stream().map(p -> CodeBlock.of("$L", p)).collect(joining(", ", call + "(", ")"));
    }

    private CodeBlock buildMethodCallable(ExecutableElement method, String call) {
        return method.getParameters().stream().map(p -> CodeBlock.of("$L", p)).collect(joining(", ", "() -> " + call + "(", ")"));
    }
}
