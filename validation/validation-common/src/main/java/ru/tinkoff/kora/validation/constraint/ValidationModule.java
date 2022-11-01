package ru.tinkoff.kora.validation.constraint;

import org.jetbrains.annotations.NotNull;
import ru.tinkoff.kora.application.graph.TypeRef;
import ru.tinkoff.kora.validation.Validator;
import ru.tinkoff.kora.validation.Violation;
import ru.tinkoff.kora.validation.constraint.factory.*;

import java.util.*;

public interface ValidationModule {

    default <T> Validator<List<T>> setValidator(Validator<T> validator, TypeRef<T> valueRef) {
        return new IterableValidator<>(validator);
    }

    default <T> Validator<List<T>> listValidator(Validator<T> validator, TypeRef<T> valueRef) {
        return new IterableValidator<>(validator);
    }

    default <T> Validator<Collection<T>> collectionValidator(Validator<T> validator, TypeRef<T> valueRef) {
        return new IterableValidator<>(validator);
    }

    default <K, V> NotEmptyValidatorFactory<Map<K, V>> notEmptyMapConstraintFactory(TypeRef<K> keyRef, TypeRef<V> valueRef) {
        return NotEmptyMapValidator::new;
    }

    default <T> NotEmptyValidatorFactory<Iterable<T>> notEmptyIterableConstraintFactory(TypeRef<T> valueRef) {
        return NotEmptyIterableValidator::new;
    }

    default <T> NotEmptyValidatorFactory<List<T>> notEmptyListConstraintFactory(TypeRef<T> valueRef) {
        return NotEmptyIterableValidator::new;
    }

    default <T> NotEmptyValidatorFactory<Set<T>> notEmptySetConstraintFactory(TypeRef<T> valueRef) {
        return NotEmptyIterableValidator::new;
    }

    default <T> NotEmptyValidatorFactory<Collection<T>> notEmptyCollectionConstraintFactory(TypeRef<T> valueRef) {
        return NotEmptyIterableValidator::new;
    }

    default NotEmptyValidatorFactory<String> notEmptyStringConstraintFactory() {
        return NotEmptyStringValidator::new;
    }

    default NotBlankValidatorFactory<String> notBlankStringConstraintFactory() {
        return NotBlankStringValidator::new;
    }

    default SizeValidatorFactory<Short> sizeShortConstraintFactory() {
        return SizeNumberValidator::new;
    }

    default SizeValidatorFactory<Integer> sizeIntConstraintFactory() {
        return SizeNumberValidator::new;
    }

    default SizeValidatorFactory<Long> sizeLongConstraintFactory() {
        return SizeNumberValidator::new;
    }

    default SizeValidatorFactory<Float> sizeFloatConstraintFactory() {
        return SizeNumberValidator::new;
    }

    default SizeValidatorFactory<Double> sizeDoubleConstraintFactory() {
        return SizeNumberValidator::new;
    }

    default SizeValidatorFactory<String> sizeStringConstraintFactory() {
        return SizeStringValidator::new;
    }

    default <K, V> SizeValidatorFactory<Map<K, V>> sizeDoubleConstraintFactory(TypeRef<K> keyRef, TypeRef<V> valueRef) {
        return SizeMapValidator::new;
    }

    default <V> SizeValidatorFactory<Collection<V>> sizeIterableConstraintFactory(TypeRef<V> valueRef) {
        return SizeCollectionValidator::new;
    }

    default <V> SizeValidatorFactory<List<V>> sizeListConstraintFactory(TypeRef<V> valueRef) {
        return SizeCollectionValidator::new;
    }

    default <V> SizeValidatorFactory<Set<V>> sizeSetConstraintFactory(TypeRef<V> valueRef) {
        return SizeCollectionValidator::new;
    }

    default NegativeValidatorFactory<Short> negativeShortConstraintFactory() {
        return NegativeNumberValidator::new;
    }

    default NegativeValidatorFactory<Integer> negativeIntConstraintFactory() {
        return NegativeNumberValidator::new;
    }

    default NegativeValidatorFactory<Long> negativeLongConstraintFactory() {
        return NegativeNumberValidator::new;
    }

    default NegativeValidatorFactory<Float> negativeFloatConstraintFactory() {
        return NegativeNumberValidator::new;
    }

    default NegativeValidatorFactory<Double> negativeDoubleConstraintFactory() {
        return NegativeNumberValidator::new;
    }

    default NegativeOrZeroValidatorFactory<Short> negativeOrZeroShortConstraintFactory() {
        return NegativeOrZeroNumberValidator::new;
    }

    default NegativeOrZeroValidatorFactory<Integer> negativeOrZeroIntConstraintFactory() {
        return NegativeOrZeroNumberValidator::new;
    }

    default NegativeOrZeroValidatorFactory<Long> negativeOrZeroLongConstraintFactory() {
        return NegativeOrZeroNumberValidator::new;
    }

    default NegativeOrZeroValidatorFactory<Float> negativeOrZeroFloatConstraintFactory() {
        return NegativeOrZeroNumberValidator::new;
    }

    default NegativeOrZeroValidatorFactory<Double> negativeOrZeroDoubleConstraintFactory() {
        return NegativeOrZeroNumberValidator::new;
    }

    default PositiveValidatorFactory<Short> positiveShortConstraintFactory() {
        return PositiveNumberValidator::new;
    }

    default PositiveValidatorFactory<Integer> positiveIntConstraintFactory() {
        return PositiveNumberValidator::new;
    }

    default PositiveValidatorFactory<Long> positiveLongConstraintFactory() {
        return PositiveNumberValidator::new;
    }

    default PositiveValidatorFactory<Float> positiveFloatConstraintFactory() {
        return PositiveNumberValidator::new;
    }

    default PositiveValidatorFactory<Double> positiveDoubleConstraintFactory() {
        return PositiveNumberValidator::new;
    }

    default PositiveOrZeroValidatorFactory<Short> positiveOrZeroShortConstraintFactory() {
        return PositiveOrZeroNumberValidator::new;
    }

    default PositiveOrZeroValidatorFactory<Integer> positiveOrZeroIntConstraintFactory() {
        return PositiveOrZeroNumberValidator::new;
    }

    default PositiveOrZeroValidatorFactory<Long> positiveOrZeroLongConstraintFactory() {
        return PositiveOrZeroNumberValidator::new;
    }

    default PositiveOrZeroValidatorFactory<Float> positiveOrZeroFloatConstraintFactory() {
        return PositiveOrZeroNumberValidator::new;
    }

    default PositiveOrZeroValidatorFactory<Double> positiveOrZeroDoubleConstraintFactory() {
        return PositiveOrZeroNumberValidator::new;
    }

    default IsTrueValidatorFactory<Boolean> isTrueBooleanConstraintFactory() {
        return () -> (value, context) -> {
            if (value == null) {
                return context.eraseAsList("Should be True, but was null");
            } else if (!value) {
                return context.eraseAsList("Should be True, but was False");
            }

            return Collections.emptyList();
        };
    }

    default IsTrueValidatorFactory<String> isTrueStringConstraintFactory() {
        return () -> (value, context) -> {
            if (value == null) {
                return context.eraseAsList("Should be True, but was null");
            } else if (!Boolean.parseBoolean(value)) {
                return context.eraseAsList("Should be True, but was False");
            }

            return Collections.emptyList();
        };
    }

    default IsFalseValidatorFactory<Boolean> isFalseBooleanConstraintFactory() {
        return () -> (value, context) -> {
            if (value == null) {
                return context.eraseAsList("Should be False, but was null");
            } else if (value) {
                return context.eraseAsList("Should be False, but was True");
            }

            return Collections.emptyList();
        };
    }

    default IsFalseValidatorFactory<String> isFalseStringConstraintFactory() {
        return () -> (value, context) -> {
            if (value == null) {
                return context.eraseAsList("Should be False, but was null");
            } else if (Boolean.parseBoolean(value)) {
                return context.eraseAsList("Should be False, but was True");
            }

            return Collections.emptyList();
        };
    }

    default PatternValidatorFactory<String> patternStringConstraintFactory() {
        return PatternValidator::new;
    }
}
