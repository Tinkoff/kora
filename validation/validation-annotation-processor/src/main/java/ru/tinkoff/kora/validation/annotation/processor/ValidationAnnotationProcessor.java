package ru.tinkoff.kora.validation.annotation.processor;

import com.squareup.javapoet.*;
import ru.tinkoff.kora.annotation.processor.common.AbstractKoraProcessor;
import ru.tinkoff.kora.validation.*;
import ru.tinkoff.kora.validation.annotation.Constrainted;
import ru.tinkoff.kora.validation.annotation.Validated;

import javax.annotation.processing.Generated;
import javax.annotation.processing.ProcessingEnvironment;
import javax.annotation.processing.RoundEnvironment;
import javax.lang.model.element.*;
import javax.lang.model.type.DeclaredType;
import javax.lang.model.type.TypeMirror;
import javax.tools.Diagnostic;
import java.io.IOException;
import java.lang.annotation.Annotation;
import java.util.*;
import java.util.stream.Collectors;

public final class ValidationAnnotationProcessor extends AbstractKoraProcessor {

    private static Set<Class<? extends Annotation>> getSupportedAnnotations() {
        return Set.of(Validated.class);
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
        final List<ValidatorMeta> validatorMetas = getValidatedElements(annotatedElements);
        for (ValidatorMeta meta : validatorMetas) {
            final TypeMirror validatorType = processingEnv.getTypeUtils().getDeclaredType(processingEnv.getElementUtils().getTypeElement(FieldValidator.class.getCanonicalName()),
                processingEnv.getElementUtils().getTypeElement(meta.source().canonicalName()).asType());

            final TypeName typeName = ParameterizedTypeName.get(validatorType);
            final TypeSpec.Builder validatorSpecBuilder = TypeSpec.classBuilder(meta.name())
                .addModifiers(Modifier.PUBLIC, Modifier.FINAL)
                .addSuperinterface(typeName)
                .addAnnotation(AnnotationSpec.builder(ClassName.get(Generated.class))
                    .addMember("value", "$S", this.getClass().getCanonicalName())
                    .build());

            final Map<ValidatorMeta.Constraint.Factory, String> constraintToFieldName = new HashMap<>();
            final Map<ValidatorMeta.Validated, String> validatedToFieldName = new HashMap<>();
            final List<CodeBlock> fieldBuilder = new ArrayList<>();
            final List<CodeBlock> constraintBuilder = new ArrayList<>();
            final List<CodeBlock> validatorBuilder = new ArrayList<>();
            for (int i = 0; i < meta.fields().size(); i++) {
                final ValidatorMeta.Field field = meta.fields().get(i);
                final String fieldName = "_f" + i;
                fieldBuilder.add(CodeBlock.of("var $L = Field.of(field.value().$L, $S, field.name());",
                    fieldName, field.accessor(), field.name()));

                for (int j = 0; j < field.constraint().size(); j++) {
                    final ValidatorMeta.Constraint constraint = field.constraint().get(j);
                    final String suffix = i + "_" + j;
                    final String violationField = "_violation" + suffix;
                    final String constraintField = constraintToFieldName.computeIfAbsent(constraint.factory(), (k) -> "_constraint" + suffix);

                    constraintBuilder.add(CodeBlock.of("""
                        final Violation $L = $L.validate($L.value());
                        if($L != null) {
                            _violations.add(Violation.of($L.message(), $L.name()));
                        }""", violationField, constraintField, fieldName, violationField, violationField, fieldName));
                }

                for (int j = 0; j < field.validates().size(); j++) {
                    final ValidatorMeta.Validated validated = field.validates().get(j);
                    final String suffix = i + "_" + j;
                    final String validatorField = validatedToFieldName.computeIfAbsent(validated, (k) -> "_validator" + suffix);

                    validatorBuilder.add(CodeBlock.of("""
                        if($L.isNotEmpty()) {
                            _violations.addAll($L.validate($L, options));
                        }""", fieldName, validatorField, fieldName));
                }
            }

            final MethodSpec.Builder constructorSpecBuilder = MethodSpec.constructorBuilder().addModifiers(Modifier.PUBLIC);
            for (var factoryToField : constraintToFieldName.entrySet()) {
                var factory = factoryToField.getKey();
                var fieldMetaType = new ValidatorMeta.Type(Constraint.class.getPackageName(), Constraint.class.getSimpleName(), factory.type().generic());
                final String fieldName = factoryToField.getValue();
                final TypeName fieldType = ParameterizedTypeName.get(fieldMetaType.asMirror(processingEnv));
                final TypeName parameterType = ParameterizedTypeName.get(factory.type().asMirror(processingEnv));
                final String createParameters = factory.parameters().values().stream()
                    .map(v -> v instanceof String ? "\"" + v + "\"" : v.toString())
                    .collect(Collectors.joining(", "));

                validatorSpecBuilder.addField(FieldSpec.builder(fieldType, fieldName, Modifier.PRIVATE, Modifier.FINAL).build());
                constructorSpecBuilder.addParameter(ParameterSpec.builder(parameterType, fieldName).build())
                    .addStatement("this.$L = $L.create($L)", fieldName, fieldName, createParameters);
            }

            for (var validatedToField : validatedToFieldName.entrySet()) {
                var validated = validatedToField.getKey();
                var fieldMetaType = ValidatorMeta.Type.ofClass(FieldValidator.class, List.of(validated.target()));
                final String fieldName = validatedToField.getValue();
                final TypeName fieldType = ParameterizedTypeName.get(fieldMetaType.asMirror(processingEnv));

                validatorSpecBuilder.addField(FieldSpec.builder(fieldType, fieldName, Modifier.PRIVATE, Modifier.FINAL).build());
                constructorSpecBuilder.addParameter(ParameterSpec.builder(fieldType, fieldName).build())
                    .addStatement("this.$L = $L", fieldName, fieldName);
            }

            final ValidatorMeta.Type validatedType = ValidatorMeta.Type.ofClass(Field.class, List.of(meta.source()));
            final MethodSpec.Builder validateMethodSpecBuilder = MethodSpec.methodBuilder("validate")
                .addAnnotation(Override.class)
                .addModifiers(Modifier.PUBLIC)
                .returns(ParameterizedTypeName.get(ValidatorMeta.Type.ofClass(List.class, List.of(ValidatorMeta.Type.ofClass(Violation.class))).asMirror(processingEnv)))
                .addParameter(ParameterSpec.builder(ParameterizedTypeName.get(validatedType.asMirror(processingEnv)), "field").build())
                .addParameter(ParameterSpec.builder(TypeName.get(ValidatorMeta.Type.ofClass(ValidationOptions.class).asMirror(processingEnv)), "options").build())
                .addStatement(CodeBlock.join(List.of(
                        CodeBlock.join(fieldBuilder, "\n"),
                        CodeBlock.of("final List<Violation> _violations = new $T<>();", ArrayList.class),
                        CodeBlock.join(constraintBuilder, "\n"),
                        CodeBlock.join(validatorBuilder, "\n"),
                        CodeBlock.of("return _violations")),
                    "\n\n"));

            final TypeSpec validatorSpec = validatorSpecBuilder
                .addMethod(constructorSpecBuilder.build())
                .addMethod(validateMethodSpecBuilder.build())
                .build();
            final PackageElement packageElement = elements.getPackageOf(meta.sourceElement());
            final JavaFile javaFile = JavaFile.builder(packageElement.getQualifiedName().toString(), validatorSpec).build();
            try {
                javaFile.writeTo(processingEnv.getFiler());
            } catch (IOException e) {
                processingEnv.getMessager().printMessage(Diagnostic.Kind.ERROR, "Error on writing file: " + e.getMessage(), meta.sourceElement());
            }
        }

        return false;
    }

    private List<ValidatorMeta> getValidatedElements(List<TypeElement> typeElements) {
        final List<ValidatorMeta> validatorMetas = new ArrayList<>();
        for (TypeElement element : typeElements) {
            final List<VariableElement> elementFields = getValidatedFields(element);
            final List<ValidatorMeta.Field> fields = new ArrayList<>();
            for (VariableElement fieldElement : elementFields) {
                final List<ValidatorMeta.Constraint> constraints = getConstraints(processingEnv, fieldElement);
                final List<ValidatorMeta.Validated> validateds = getValidated(fieldElement);

                if (!(constraints.isEmpty() && validateds.isEmpty())) {
                    ValidatorMeta.Type.ofType(fieldElement.asType());
                    final ValidatorMeta.Field fieldMeta = new ValidatorMeta.Field(
                        ValidatorMeta.Type.ofType(fieldElement.asType()),
                        fieldElement.getSimpleName().toString(),
                        element.getKind() == ElementKind.RECORD,
                        constraints,
                        validateds);

                    fields.add(fieldMeta);
                }
            }

            final ValidatorMeta meta = new ValidatorMeta(ValidatorMeta.Type.ofType(element.asType()), element, fields);
            validatorMetas.add(meta);
        }

        return validatorMetas;
    }

    private static List<VariableElement> getValidatedFields(TypeElement element) {
        return element.getEnclosedElements().stream()
            .filter(e -> e.getKind() == ElementKind.FIELD)
            .filter(e -> e instanceof VariableElement)
            .map(e -> ((VariableElement) e))
            .toList();
    }

    private static List<ValidatorMeta.Constraint> getConstraints(ProcessingEnvironment env, VariableElement field) {
        return field.getAnnotationMirrors().stream()
            .flatMap(annotation -> annotation.getAnnotationType().asElement().getAnnotationMirrors().stream()
                .filter(innerAnnotation -> innerAnnotation.getAnnotationType().toString().equals(Constrainted.class.getCanonicalName()))
                .flatMap(constainted -> constainted.getElementValues().entrySet().stream()
                    .filter(entry -> entry.getKey().getSimpleName().contentEquals("value"))
                    .map(en -> en.getValue().getValue())
                    .filter(v -> v instanceof DeclaredType)
                    .map(v -> {
                        final DeclaredType factoryRawType = (DeclaredType) v;
                        final Map<String, Object> parameters = annotation.getElementValues().entrySet().stream()
                            .collect(Collectors.toMap(ae -> ae.getKey().getSimpleName().toString(), ae -> {
                                final Object value = ae.getValue().getValue();
                                return (value instanceof List<?>)
                                    ? ((List<?>) value).stream().map(ValidationAnnotationProcessor::castParameterValue).toList()
                                    : castParameterValue(value);
                            }));

                        final DeclaredType factoryDeclaredType = env.getTypeUtils().getDeclaredType((TypeElement) factoryRawType.asElement(), field.asType());
                        final ValidatorMeta.Type factoryGenericType = ValidatorMeta.Type.ofType(factoryDeclaredType);

                        final ValidatorMeta.Constraint.Factory constraintFactory = new ValidatorMeta.Constraint.Factory(
                            factoryGenericType,
                            factoryRawType.asElement().getKind().isClass(),
                            parameters);

                        final ValidatorMeta.Type annotationType = ValidatorMeta.Type.ofType(annotation.getAnnotationType().asElement().asType());
                        return new ValidatorMeta.Constraint(annotationType, constraintFactory);
                    })))
            .toList();
    }

    private static List<ValidatorMeta.Validated> getValidated(VariableElement field) {
        return isAnyValidated(field.asType())
            ? List.of(new ValidatorMeta.Validated(ValidatorMeta.Type.ofType(field.asType())))
            : Collections.emptyList();
    }

    private static boolean isAnyValidated(TypeMirror typeMirror) {
        if (typeMirror instanceof DeclaredType dt) {
            if (dt.asElement().getAnnotationMirrors().stream().anyMatch(a -> a.getAnnotationType().toString().equals(Validated.class.getCanonicalName()))) {
                return true;
            }

            return dt.getTypeArguments().stream().anyMatch(ValidationAnnotationProcessor::isAnyValidated);
        }

        return typeMirror.getAnnotationMirrors().stream().anyMatch(a -> a.getAnnotationType().toString().equals(Validated.class.getCanonicalName()));
    }

    private List<TypeElement> getAnnotatedElements(RoundEnvironment roundEnv) {
        return getSupportedAnnotations().stream()
            .flatMap(a -> roundEnv.getElementsAnnotatedWith(a).stream())
            .filter(a -> a instanceof TypeElement)
            .map(a -> ((TypeElement) a))
            .toList();
    }

    private static Object castParameterValue(Object value) {
        if (value instanceof String) {
            return value;
        }

        if (value instanceof Number) {
            return value;
        }

        return value.toString();
    }
}
