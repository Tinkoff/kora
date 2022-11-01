package ru.tinkoff.kora.validation.constraint;

import org.jetbrains.annotations.NotNull;
import ru.tinkoff.kora.validation.ValidationContext;
import ru.tinkoff.kora.validation.Validator;
import ru.tinkoff.kora.validation.Violation;
import ru.tinkoff.kora.validation.annotation.Range;

import java.util.Collections;
import java.util.List;

final class RangeNumberValidator<T extends Number> implements Validator<T> {

    private final double from;
    private final double to;
    private final Range.Boundary boundary;

    RangeNumberValidator(double from, double to, Range.Boundary boundary) {
        this.from = from;
        this.to = to;
        this.boundary = boundary;
    }

    @NotNull
    @Override
    public List<Violation> validate(T value, @NotNull ValidationContext context) {
        if (value == null) {
            return List.of(context.violates("Should be in range from '" + from + "' to '" + to + "', but was null"));
        } else if (value.longValue() < from) {
            return List.of(context.violates("Should be in range from '" + from + "' to '" + to + "', but was smaller: " + value));
        } else if (value.longValue() > to) {
            return List.of(context.violates("Should be in range from '" + from + "' to '" + to + "', but was greater: " + value));
        }

        return Collections.emptyList();
    }
}
