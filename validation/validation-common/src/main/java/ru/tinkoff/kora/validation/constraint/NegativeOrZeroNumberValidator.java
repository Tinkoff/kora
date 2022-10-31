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
final class NegativeOrZeroNumberValidator<T extends Number> implements Validator<T> {

    @NotNull
    @Override
    public List<Violation> validate(T value, @NotNull ValidationContext context) {
        if (value == null) {
            return context.eraseAsList("Should not negative or zero, but was null");
        } else if (value.longValue() > 0 || value.doubleValue() > 0) {
            return context.eraseAsList("Should be negative or zero, but was: " + value);
        }

        return Collections.emptyList();
    }
}
