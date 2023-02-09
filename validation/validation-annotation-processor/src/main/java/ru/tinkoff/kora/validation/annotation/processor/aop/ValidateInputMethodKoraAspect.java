package ru.tinkoff.kora.validation.annotation.processor.aop;

import com.squareup.javapoet.ClassName;
import com.squareup.javapoet.CodeBlock;
import ru.tinkoff.kora.annotation.processor.common.CommonUtils;
import ru.tinkoff.kora.annotation.processor.common.MethodUtils;
import ru.tinkoff.kora.annotation.processor.common.ProcessingErrorException;
import ru.tinkoff.kora.aop.annotation.processor.KoraAspect;
import ru.tinkoff.kora.validation.annotation.processor.ValidMeta;
import ru.tinkoff.kora.validation.annotation.processor.ValidUtils;

import javax.annotation.processing.ProcessingEnvironment;
import javax.lang.model.element.*;
import javax.lang.model.type.PrimitiveType;
import javax.lang.model.type.TypeKind;
import java.util.*;
import java.util.concurrent.Future;

import static com.squareup.javapoet.CodeBlock.joining;
import static ru.tinkoff.kora.validation.annotation.processor.ValidMeta.*;

public class ValidateInputMethodKoraAspect implements KoraAspect {

    private static final ClassName VALIDATED_INPUT_TYPE = ClassName.get("ru.tinkoff.kora.validation.common.annotation", "ValidateInput");

    private final ProcessingEnvironment env;

    public ValidateInputMethodKoraAspect(ProcessingEnvironment env) {
        this.env = env;
    }

    @Override
    public Set<String> getSupportedAnnotationTypes() {
        return Set.of(VALIDATED_INPUT_TYPE.canonicalName());
    }

    @Override
    public ApplyResult apply(ExecutableElement method, String superCall, AspectContext aspectContext) {
        if (MethodUtils.isFuture(method, env)) {
            throw new ProcessingErrorException("@ValidateInput can't be applied for types assignable from " + Future.class, method);
        }

        final boolean isValidated = method.getAnnotationMirrors().stream().anyMatch(a -> a.getAnnotationType().toString().equals(VALID_TYPE.canonicalName()));
        final boolean isAnyParameterValidated = method.getParameters().stream().anyMatch(ValidateInputMethodKoraAspect::isValidatable);
        if (isValidated && !isAnyParameterValidated) {
            throw new ProcessingErrorException("@ValidateInput is present on method declaration, but no Method Arguments validation declarations present", method);
        }

        var codeBlock = CodeBlock.builder()
            .addStatement("var _violations = new $T<$T>($L)", ArrayList.class, VIOLATION_TYPE, method.getParameters().size() * 2);

        for (var parameter : method.getParameters()) {
            var isNullable = CommonUtils.isNullable(parameter);
            var isPrimitive = parameter.asType() instanceof PrimitiveType;
            if (isValidatable(parameter)) {
                var validates = getValidated(parameter);
                var constraints = ValidUtils.getValidatedByConstraints(env, parameter.asType(), parameter.getAnnotationMirrors());

                for (var validated : validates) {
                    var validatorType = validated.validator().asMirror(env);

                    var validatorField = aspectContext.fieldFactory().constructorParam(validatorType, List.of());
                    if (isNullable && !isPrimitive) {
                        codeBlock.beginControlFlow("if($N != null)", parameter.getSimpleName());
                        codeBlock.addStatement("_violations.addAll($N.validate($N))", validatorField, parameter.getSimpleName());
                        codeBlock.endControlFlow();
                    } else {
                        codeBlock.addStatement("_violations.addAll($N.validate($N))", validatorField, parameter.getSimpleName());
                    }
                }

                for (var constraint : constraints) {
                    var constraintFactory = aspectContext.fieldFactory().constructorParam(constraint.factory().type().asMirror(env), List.of());
                    var constraintType = constraint.factory().validator().asMirror(env);

                    final CodeBlock createExec = CodeBlock.builder()
                        .add("$N.create", constraintFactory)
                        .add(constraint.factory().parameters().values().stream()
                            .map(fp -> CodeBlock.of("$L", fp))
                            .collect(joining(", ", "(", ")")))
                        .build();

                    var constraintField = aspectContext.fieldFactory().constructorInitialized(constraintType, createExec);
                    if (isNullable && !isPrimitive) {
                        codeBlock.beginControlFlow("if($N != null)", parameter.getSimpleName());
                        codeBlock.addStatement("_violations.addAll($N.validate($N))", constraintField, parameter.getSimpleName());
                        codeBlock.endControlFlow();
                    } else {
                        codeBlock.addStatement("_violations.addAll($N.validate($N))", constraintField, parameter.getSimpleName());
                    }
                }
            }
        }

        codeBlock.beginControlFlow("if (!_violations.isEmpty())")
            .addStatement("throw new $T(_violations)", EXCEPTION_TYPE)
            .endControlFlow();

        if (method.getReturnType().getKind() != TypeKind.VOID) {
            codeBlock.add("return ");
        }

        codeBlock.add(method.getParameters().stream()
            .map(p -> CodeBlock.of("$N", p.getSimpleName()))
            .collect(joining(", ", superCall + "(", ");\n")));

        return new ApplyResult.MethodBody(codeBlock.build());
    }

    private static boolean isValidatable(VariableElement parameter) {
        for (var annotation : parameter.getAnnotationMirrors()) {
            var annotationType = annotation.getAnnotationType();
            if (annotationType.toString().equals(VALID_TYPE.canonicalName())) {
                return true;
            }

            for (var innerAnnotation : annotationType.asElement().getAnnotationMirrors()) {
                if (innerAnnotation.getAnnotationType().toString().equals(VALIDATED_BY_TYPE.canonicalName())) {
                    return true;
                }
            }
        }

        return false;
    }

    private static List<ValidMeta.Validated> getValidated(VariableElement parameter) {
        if (parameter.getAnnotationMirrors().stream().anyMatch(a -> a.getAnnotationType().toString().equals(VALID_TYPE.canonicalName()))) {
            return List.of(new ValidMeta.Validated(ValidMeta.Type.ofType(parameter.asType())));
        }

        return Collections.emptyList();
    }
}
