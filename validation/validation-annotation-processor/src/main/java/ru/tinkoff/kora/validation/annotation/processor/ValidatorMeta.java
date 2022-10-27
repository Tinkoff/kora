package ru.tinkoff.kora.validation.annotation.processor;

import javax.annotation.processing.ProcessingEnvironment;
import javax.lang.model.element.TypeElement;
import javax.lang.model.type.DeclaredType;
import javax.lang.model.type.TypeMirror;
import java.util.Collections;
import java.util.List;
import java.util.Map;
import java.util.Objects;

public record ValidatorMeta(Type source, TypeElement sourceElement, List<Field> fields) {

    public String name() {
        return "$Validator_" + source().simpleName();
    }

    public record Field(Type type, String name, boolean isRecord, List<Constraint> constraint, List<Validated> validates) {

        public String accessor() {
            return (isRecord)
                ? name + "()"
                : "get" + name.substring(0, 1).toUpperCase() + name.substring(1) + "()";
        }
    }

    public record Validated(Type target) { }

    public record Constraint(Type annotation, Factory factory) {

        public record Factory(Type type, boolean isClass, Map<String, Object> parameters) { }
    }

    public record Type(String packageName, String simpleName, List<Type> generic) {

        public static Type ofType(TypeMirror type) {
            if(type instanceof DeclaredType) {
                return ofType(((DeclaredType) type));
            }

            final String canonicalName = type.toString();
            return new Type(canonicalName.substring(0, canonicalName.lastIndexOf('.')),
                canonicalName.substring(canonicalName.lastIndexOf('.') + 1),
                Collections.emptyList());
        }

        public static Type ofType(DeclaredType type) {
            final List<Type> generics = type.getTypeArguments().stream()
                .map(Type::ofType)
                .toList();

            final String canonicalName = type.asElement().toString();
            return new Type(canonicalName.substring(0, canonicalName.lastIndexOf('.')),
                canonicalName.substring(canonicalName.lastIndexOf('.') + 1),
                generics);
        }

        public static Type ofClass(Class<?> clazz) {
            return new Type(clazz.getPackageName(), clazz.getSimpleName(), Collections.emptyList());
        }

        public static Type ofClass(Class<?> clazz, List<Type> generic) {
            return new Type(clazz.getPackageName(), clazz.getSimpleName(), generic);
        }

        public static Type ofName(String canonicalName) {
            return new Type(canonicalName.substring(0, canonicalName.lastIndexOf('.')),
                canonicalName.substring(canonicalName.lastIndexOf('.') + 1),
                Collections.emptyList());
        }

        public String canonicalName() {
            return packageName + "." + simpleName;
        }

        public TypeMirror asMirror(ProcessingEnvironment env) {
            if(generic.isEmpty()) {
                return env.getTypeUtils().getDeclaredType(env.getElementUtils().getTypeElement(canonicalName()));
            } else {
                final TypeMirror[] generics = generic().stream()
                    .map(g -> g.asMirror(env))
                    .toArray(TypeMirror[]::new);

                return env.getTypeUtils().getDeclaredType(env.getElementUtils().getTypeElement(canonicalName()), generics);
            }
        }

        @Override
        public String toString() {
            return canonicalName();
        }

        @Override
        public boolean equals(Object o) {
            if (this == o) return true;
            if (!(o instanceof Type)) return false;
            Type type = (Type) o;
            return Objects.equals(packageName, type.packageName) && Objects.equals(simpleName, type.simpleName) && Objects.equals(generic, type.generic);
        }

        @Override
        public int hashCode() {
            return Objects.hash(packageName, simpleName, generic);
        }
    }
}
