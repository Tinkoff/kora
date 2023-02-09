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
import javax.lang.model.element.ExecutableElement;
import javax.lang.model.element.VariableElement;
import javax.lang.model.type.PrimitiveType;
import javax.lang.model.type.TypeMirror;
import java.util.*;
import java.util.concurrent.Future;

import static com.squareup.javapoet.CodeBlock.joining;
import static ru.tinkoff.kora.validation.annotation.processor.ValidMeta.*;

public class ValidateOutputMethodKoraAspect implements KoraAspect {

    private static final ClassName VALIDATED_INPUT_TYPE = ClassName.get("ru.tinkoff.kora.validation.common.annotation", "ValidateOutput");

    private final ProcessingEnvironment env;

    public ValidateOutputMethodKoraAspect(ProcessingEnvironment env) {
        this.env = env;
    }

    @Override
    public Set<String> getSupportedAnnotationTypes() {
        return Set.of(VALIDATED_INPUT_TYPE.canonicalName());
    }

    @Override
    public ApplyResult apply(ExecutableElement method, String superCall, AspectContext aspectContext) {
        if (MethodUtils.isFuture(method, env)) {
            throw new ProcessingErrorException("@ValidateOutput can't be applied for types assignable from " + Future.class, method);
        }

        final boolean isMono = MethodUtils.isMono(method, env);
        final boolean isFlux = MethodUtils.isFlux(method, env);
        if (MethodUtils.isVoid(method)) {
            throw new ProcessingErrorException("@ValidateOutput can't be applied for types assignable from " + Void.class, method);
        } else if (isMono || isFlux) {
            if (MethodUtils.getGenericType(method.getReturnType()).filter(MethodUtils::isVoid).isPresent()) {
                throw new ProcessingErrorException("@ValidateOutput can't be applied for types assignable from " + Void.class, method);
            }
        }

        final TypeMirror returnType = (isMono || isFlux)
            ? MethodUtils.getGenericType(method.getReturnType()).orElseThrow()
            : method.getReturnType();

        var validationBody = buildValidationCodeBlock(method, returnType, aspectContext);

        final CodeBlock body;
        if (isMono) {
            body = buildBodyMono(method, superCall, validationBody);
        } else if (isFlux) {
            body = buildBodyFlux(method, superCall, validationBody);
        } else {
            body = buildBodySync(method, superCall, validationBody);
        }

        return new ApplyResult.MethodBody(body);
    }

    private CodeBlock buildValidationCodeBlock(ExecutableElement method, TypeMirror returnType, AspectContext aspectContext) {
        final boolean isMono = MethodUtils.isMono(method, env);
        final boolean isFlux = MethodUtils.isFlux(method, env);

        final List<ValidMeta.Constraint> constraints = ValidUtils.getValidatedByConstraints(env, returnType, method.getAnnotationMirrors());
        final List<Validated> validates = (method.getAnnotationMirrors().stream().anyMatch(a -> a.getAnnotationType().toString().equals(VALID_TYPE.canonicalName())))
            ? List.of(new ValidMeta.Validated(ValidMeta.Type.ofType(returnType)))
            : Collections.emptyList();

        if(constraints.isEmpty() && validates.isEmpty()) {
            throw new ProcessingErrorException("@ValidateOutput is present on method declaration, but no Return Type validation declarations present", method);
        }

        var isNullable = CommonUtils.isNullable(method);
        var isPrimitive = returnType instanceof PrimitiveType;

        var validationBody = CodeBlock.builder()
            .addStatement("var _violations = new $T<$T>($L)", ArrayList.class, VIOLATION_TYPE, method.getParameters().size() * 2);

        for (var validated : validates) {
            var validatorType = validated.validator().asMirror(env);
            var validatorField = aspectContext.fieldFactory().constructorParam(validatorType, List.of());
            if (isNullable && !isPrimitive && !isMono && !isFlux) {
                validationBody.beginControlFlow("if(_result != null)");
                validationBody.addStatement("_violations.addAll($N.validate(_result))", validatorField);
                validationBody.endControlFlow();
            } else {
                validationBody.addStatement("_violations.addAll($N.validate(_result))", validatorField);
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
            if (isNullable && !isPrimitive && !isMono && !isFlux) {
                validationBody.beginControlFlow("if($N != null)");
                validationBody.addStatement("_violations.addAll($N.validate(_result))", constraintField);
                validationBody.endControlFlow();
            } else {
                validationBody.addStatement("_violations.addAll($N.validate(_result))", constraintField);
            }
        }

        validationBody.beginControlFlow("if (!_violations.isEmpty())")
            .addStatement("throw new $T(_violations)", EXCEPTION_TYPE)
            .endControlFlow();

        return validationBody.build();
    }

    private CodeBlock buildBodySync(ExecutableElement method, String superCall, CodeBlock validationBlock) {
        final boolean isNullable = CommonUtils.isNullable(method);
        final CodeBlock superMethod = buildMethodCall(method, superCall);
        var builder = CodeBlock.builder();
        if (isNullable) {
            builder.add("""
                var _result = $L;
                """, superMethod.toString())
                .add(validationBlock)
                .build();
        } else {
            builder.add("""
                var _result = $L;
                """, superMethod.toString())
                .add(validationBlock)
                .build();
        }

        if (MethodUtils.isVoid(method)) {
            return builder.build();
        } else {
            return builder
                .add("return _result;")
                .build();
        }
    }

    private CodeBlock buildBodyMono(ExecutableElement method, String superCall, CodeBlock validationBlock) {
        final CodeBlock superMethod = buildMethodCall(method, superCall);
        return CodeBlock.builder()
            .add("""
                return $L
                    .map(_result -> {
                    """, superMethod.toString())
            .indent()
            .indent()
            .indent()
            .add(validationBlock)
            .add("return _result;")
            .unindent()
            .unindent()
            .unindent()
            .add("""
                
                });""")
            .build();
    }

    private CodeBlock buildBodyFlux(ExecutableElement method, String superCall, CodeBlock validationBlock) {
        final CodeBlock superMethod = buildMethodCall(method, superCall);
        return CodeBlock.builder()
            .add("""
                return $L
                    .map(_result -> {
                    """, superMethod.toString())
            .indent()
            .indent()
            .indent()
            .add(validationBlock)
            .add("return _result;")
            .unindent()
            .unindent()
            .unindent()
            .add("""
                
                });""")
            .build();
    }

    private CodeBlock buildMethodCall(ExecutableElement method, String call) {
        return method.getParameters().stream().map(p -> CodeBlock.of("$L", p)).collect(joining(", ", call + "(", ")"));
    }
}
