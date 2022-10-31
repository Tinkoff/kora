package ru.tinkoff.kora.validation.constraint;

import org.jetbrains.annotations.NotNull;
import ru.tinkoff.kora.validation.ValidationContext;
import ru.tinkoff.kora.validation.Validator;
import ru.tinkoff.kora.validation.Violation;

import java.util.Collections;
import java.util.List;

/**
 * Please add Description Here.
 */
final class SizeNumberValidator<T extends Number> implements Validator<T> {

    private final long from;
    private final long to;

    public SizeNumberValidator(long from, long to) {
        this.from = from;
        this.to = to;
    }

    @NotNull
    @Override
    public List<Violation> validate(T value, @NotNull ValidationContext context) {
        if (value == null) {
            return context.eraseAsList("Should be in range from '" + from + "' to '" + to + "', but was null");
        } else if (value.longValue() < from) {
            return context.eraseAsList("Should be in range from '" + from + "' to '" + to + "', but was smaller: " + value);
        } else if (value.longValue() > to) {
            return context.eraseAsList("Should be in range from '" + from + "' to '" + to + "', but was greater: " + value);
        }

        return Collections.emptyList();
    }
}
