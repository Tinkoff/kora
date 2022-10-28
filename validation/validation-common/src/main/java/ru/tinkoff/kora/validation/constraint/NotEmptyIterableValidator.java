package ru.tinkoff.kora.validation.constraint;

import org.jetbrains.annotations.NotNull;
import ru.tinkoff.kora.validation.ValidationContext;
import ru.tinkoff.kora.validation.Validator;
import ru.tinkoff.kora.validation.Violation;

import java.util.Collections;
import java.util.List;

public final class NotEmptyIterableValidator<V, T extends Iterable<V>> implements Validator<T> {

    @NotNull
    @Override
    public List<Violation> validate(T value, @NotNull ValidationContext context) {
        if (value == null) {
            return context.eraseAsList("Should be not empty, but was null");
        }

        if (!value.iterator().hasNext()) {
            return context.eraseAsList("Should be not empty, but was empty");
        }

        return Collections.emptyList();
    }
}
