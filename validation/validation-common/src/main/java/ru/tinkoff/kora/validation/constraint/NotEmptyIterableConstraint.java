package ru.tinkoff.kora.validation.constraint;

import ru.tinkoff.kora.validation.Constraint;
import ru.tinkoff.kora.validation.Violation;

public final class NotEmptyIterableConstraint<V, T extends Iterable<V>> implements Constraint<T> {

    @Override
    public Violation validate(T fieldValue) {
        if (fieldValue == null) {
            return Violation.of("Should be not empty, but was null");
        }

        if (!fieldValue.iterator().hasNext()) {
            return Violation.of("Should be not empty, but was empty");
        }

        return null;
    }
}
