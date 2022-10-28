package ru.tinkoff.kora.validation.constraint;

import org.jetbrains.annotations.NotNull;
import ru.tinkoff.kora.validation.ValidationContext;
import ru.tinkoff.kora.validation.Validator;
import ru.tinkoff.kora.validation.Violation;

import java.util.Collections;
import java.util.List;
import java.util.Map;

/**
 * Please add Description Here.
 */
public final class NotEmptyMapValidator<K, V> implements Validator<Map<K, V>> {

    @NotNull
    @Override
    public List<Violation> validate(Map<K, V> value, @NotNull ValidationContext context) {
        if (value == null) {
            return context.eraseAsList("Should be not empty, but was null");
        }

        if (value.isEmpty()) {
            return context.eraseAsList("Should be not empty, but was empty");
        }

        return Collections.emptyList();
    }
}
