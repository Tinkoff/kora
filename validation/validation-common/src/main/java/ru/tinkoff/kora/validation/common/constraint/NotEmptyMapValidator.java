package ru.tinkoff.kora.validation.common.constraint;

import org.jetbrains.annotations.NotNull;
import ru.tinkoff.kora.validation.common.ValidationContext;
import ru.tinkoff.kora.validation.common.Validator;
import ru.tinkoff.kora.validation.common.Violation;

import java.util.Collections;
import java.util.List;
import java.util.Map;

final class NotEmptyMapValidator<K, V> implements Validator<Map<K, V>> {

    @NotNull
    @Override
    public List<Violation> validate(Map<K, V> value, @NotNull ValidationContext context) {
        if (value == null) {
            return List.of(context.violates("Should be not empty, but was null"));
        } else if (value.isEmpty()) {
            return List.of(context.violates("Should be not empty, but was empty"));
        }

        return Collections.emptyList();
    }
}
