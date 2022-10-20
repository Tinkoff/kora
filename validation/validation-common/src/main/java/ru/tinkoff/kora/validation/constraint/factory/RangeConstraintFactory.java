package ru.tinkoff.kora.validation.constraint.factory;

import ru.tinkoff.kora.validation.Constraint;
import ru.tinkoff.kora.validation.ConstraintFactory;

/**
 * Please add Description Here.
 */
public interface RangeConstraintFactory<T> extends ConstraintFactory<T> {

    @Override
    default Constraint<T> create() {
        return create(Long.MIN_VALUE, Long.MAX_VALUE);
    }

    Constraint<T> create(Long from, Long to);
}
