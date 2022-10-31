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
final class SizeStringValidator implements Validator<String> {

    private final int from;
    private final int to;

    public SizeStringValidator(long from, long to) {
        this.from = Long.valueOf(from).intValue();
        this.to =Long.valueOf(to).intValue();
    }

    @NotNull
    @Override
    public List<Violation> validate(String value, @NotNull ValidationContext context) {
        if (value == null) {
            return context.eraseAsList("Length should be in range from '" + from + "' to '" + to + "', but was null");
        } else if (value.length() < from) {
            return context.eraseAsList("Length should be in range from '" + from + "' to '" + to + "', but was smaller: " + value.length());
        } else if (value.length() > to) {
            return context.eraseAsList("Length should be in range from '" + from + "' to '" + to + "', but was greater: " + value.length());
        }

        return Collections.emptyList();
    }
}
