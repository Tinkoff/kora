package ru.tinkoff.kora.validation.constraint;

import org.jetbrains.annotations.NotNull;
import ru.tinkoff.kora.validation.ValidationContext;
import ru.tinkoff.kora.validation.Validator;
import ru.tinkoff.kora.validation.Violation;

import java.util.Collections;
import java.util.List;

final class NotBlankStringValidator implements Validator<String> {

    @NotNull
    @Override
    public List<Violation> validate(String value, @NotNull ValidationContext context) {
        if (value == null) {
            return List.of(context.violates("Should be not blank, but was null"));
        } else if (value.isEmpty()) {
            return List.of(context.violates("Should be not blank, but was empty"));
        } else if (value.isBlank()) {
            return List.of(context.violates("Should be not blank, but was blank"));
        }

        return Collections.emptyList();
    }
}
