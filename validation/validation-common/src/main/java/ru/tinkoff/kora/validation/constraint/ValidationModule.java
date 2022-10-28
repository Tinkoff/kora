package ru.tinkoff.kora.validation.constraint;

import ru.tinkoff.kora.application.graph.TypeRef;
import ru.tinkoff.kora.validation.Validator;
import ru.tinkoff.kora.validation.Violation;
import ru.tinkoff.kora.validation.constraint.factory.NotEmptyValidatorFactory;
import ru.tinkoff.kora.validation.constraint.factory.RangeValidatorFactory;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.Map;

public interface ValidationModule {

    default <T> Validator<List<T>> listValidator(Validator<T> validator, TypeRef<T> valueRef) {
        return (value, context) -> {
            if (value != null) {
                final List<Violation> violations = new ArrayList<>();
                for (int i = 0; i < value.size(); i++) {
                    final T t = value.get(i);
                    violations.addAll(validator.validate(t, context.addPath("[" + i + "]")));
                }
                return violations;
            }

            return Collections.emptyList();
        };
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

    default NotEmptyValidatorFactory<String> notEmptyStringConstraintFactory() {
        return NotEmptyStringValidator::new;
    }

    default RangeValidatorFactory<Integer> rangeIntConstraintFactory() {
        return NumberRangeValidator::new;
    }

    default RangeValidatorFactory<Long> rangeLongConstraintFactory() {
        return NumberRangeValidator::new;
    }
}
