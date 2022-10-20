package ru.tinkoff.kora.validation.constraint;

import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;
import ru.tinkoff.kora.application.graph.TypeRef;
import ru.tinkoff.kora.common.Module;
import ru.tinkoff.kora.validation.*;
import ru.tinkoff.kora.validation.constraint.factory.NotEmptyConstraintFactory;
import ru.tinkoff.kora.validation.constraint.factory.NotNullConstraintFactory;
import ru.tinkoff.kora.validation.constraint.factory.RangeConstraintFactory;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;

@Module
public interface ValidationModule {

    default <T> FieldValidator<List<T>> listValidator(FieldValidator<T> validator, TypeRef<T> valueRef) {
        return (field, options) -> {
            final List<Violation> violations = new ArrayList<>();
            for (int i = 0; i < field.value().size(); i++) {
                final T value = field.value().get(i);
                var v = Field.of(value, "[" + i + "]", field.name());
                violations.addAll(validator.validate(v, options));
            }
            return violations;
        };
    }

    default <K, V> NotEmptyConstraintFactory<Map<K, V>> notEmptyMapConstraintFactory(TypeRef<K> keyRef, TypeRef<V> valueRef) {
        return NotEmptyMapConstraint::new;
    }

    default <T> NotEmptyConstraintFactory<Iterable<T>> notEmptyIterableConstraintFactory(TypeRef<T> valueRef) {
        return NotEmptyIterableConstraint::new;
    }

    default <T> NotEmptyConstraintFactory<List<T>> notEmptyListConstraintFactory(TypeRef<T> valueRef) {
        return NotEmptyIterableConstraint::new;
    }

    default NotEmptyConstraintFactory<String> notEmptyStringConstraintFactory() {
        return NotEmptyStringConstraint::new;
    }

    default NotNullConstraintFactory<String> notNullConstraintFactory() {
        return NotNullConstraint::new;
    }

    default RangeConstraintFactory<Integer> rangeIntConstraintFactory() {
        return NumberRangeConstraint::new;
    }

    default RangeConstraintFactory<Long> rangeLongConstraintFactory() {
        return NumberRangeConstraint::new;
    }
}
