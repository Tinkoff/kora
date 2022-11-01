package ru.tinkoff.kora.validation.constraint;

import org.jetbrains.annotations.NotNull;
import ru.tinkoff.kora.validation.ValidationContext;
import ru.tinkoff.kora.validation.Validator;
import ru.tinkoff.kora.validation.Violation;

import java.util.Collections;
import java.util.List;

final class SizeStringValidator implements Validator<String> {

    private final int from;
    private final int to;

    public SizeStringValidator(int from, int to) {
        if (from < 0)
            throw new IllegalArgumentException("From can't be less 0, but was: " + from);

        this.from = from;
        this.to = to;
    }

    @NotNull
    @Override
    public List<Violation> validate(String value, @NotNull ValidationContext context) {
        if (value == null) {
            return List.of(context.violates("Length should be in range from '" + from + "' to '" + to + "', but was null"));
        } else if (value.length() < from) {
            return List.of(context.violates("Length should be in range from '" + from + "' to '" + to + "', but was smaller: " + value.length()));
        } else if (value.length() > to) {
            return List.of(context.violates("Length should be in range from '" + from + "' to '" + to + "', but was greater: " + value.length()));
        }

        return Collections.emptyList();
    }
}
