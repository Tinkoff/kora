package ru.tinkoff.kora.validation.constraint.factory;

import org.jetbrains.annotations.NotNull;
import ru.tinkoff.kora.validation.Constraint;
import ru.tinkoff.kora.validation.ConstraintFactory;

/**
 * Please add Description Here.
 */
public interface RangeConstraintFactory<T> extends ConstraintFactory<T> {

    @NotNull
    @Override
    default Constraint<T> create() {
        return create(Long.MIN_VALUE, Long.MAX_VALUE);
    }

    @NotNull
    Constraint<T> create(long from, long to);
}
