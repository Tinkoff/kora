package ru.tinkoff.kora.validation.constraint;

import ru.tinkoff.kora.application.graph.TypeRef;
import ru.tinkoff.kora.validation.Validator;
import ru.tinkoff.kora.validation.constraint.factory.*;

import java.util.Collection;
import java.util.List;
import java.util.Map;
import java.util.Set;

public interface ValidationModule {

    default <T> Validator<List<T>> listValidator(Validator<T> validator, TypeRef<T> valueRef) {
        return new IterableValidator<>(validator);
    }

    default <T> Validator<Set<T>> setValidator(Validator<T> validator, TypeRef<T> valueRef) {
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

    default RangeValidatorFactory<Short> sizeShortConstraintFactory() {
        return RangeNumberValidator::new;
    }

    default RangeValidatorFactory<Integer> sizeIntConstraintFactory() {
        return RangeNumberValidator::new;
    }

    default RangeValidatorFactory<Long> sizeLongConstraintFactory() {
        return RangeNumberValidator::new;
    }

    default RangeValidatorFactory<Float> sizeFloatConstraintFactory() {
        return RangeNumberValidator::new;
    }

    default RangeValidatorFactory<Double> sizeDoubleConstraintFactory() {
        return RangeNumberValidator::new;
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

    default PatternValidatorFactory<String> patternStringConstraintFactory() {
        return PatternValidator::new;
    }
}
