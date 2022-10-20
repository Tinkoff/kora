package ru.tinkoff.kora.validation.constraint;

import ru.tinkoff.kora.validation.Constraint;
import ru.tinkoff.kora.validation.Violation;

/**
 * Please add Description Here.
 */
public final class NumberRangeConstraint<T extends Number> implements Constraint<T> {

    private final long from;
    private final long to;

    public NumberRangeConstraint(long from, long to) {
        this.from = from;
        this.to = to;
    }

    @Override
    public Violation validate(T fieldValue) {
        if(fieldValue.longValue() < from) {
            return Violation.of("Should be in range from '" + from + "' to '" + to + "', but was smaller: " + fieldValue);
        }

        if(fieldValue.longValue() > to) {
            return Violation.of("Should be in range from '" + from + "' to '" + to + "', but was greater: " + fieldValue);
        }

        return null;
    }
}
