package ru.tinkoff.kora.validation.constraint;

import org.jetbrains.annotations.NotNull;
import ru.tinkoff.kora.validation.ValidationContext;
import ru.tinkoff.kora.validation.Validator;
import ru.tinkoff.kora.validation.Violation;

import java.util.Collection;
import java.util.Collections;
import java.util.List;
import java.util.Map;

/**
 * Please add Description Here.
 */
final class SizeMapValidator<K, V> implements Validator<Map<K, V>> {

    private final int from;
    private final int to;

    public SizeMapValidator(long from, long to) {
        this.from = Long.valueOf(from).intValue();
        this.to = Long.valueOf(to).intValue();
    }

    @NotNull
    @Override
    public List<Violation> validate(Map<K, V> value, @NotNull ValidationContext context) {
        if (value == null) {
            return context.eraseAsList("Size should be in range from '" + from + "' to '" + to + "', but value was null");
        } else if (value.size() < from) {
            return context.eraseAsList("Size should be in range from '" + from + "' to '" + to + "', but was smaller: " + value.size());
        } else if (value.size() > to) {
            return context.eraseAsList("Size should be in range from '" + from + "' to '" + to + "', but was greater: " + value.size());
        }

        return Collections.emptyList();
    }
}
