package ru.tinkoff.kora.validation.constraint;

import org.jetbrains.annotations.NotNull;
import ru.tinkoff.kora.validation.ValidationContext;
import ru.tinkoff.kora.validation.Validator;
import ru.tinkoff.kora.validation.Violation;

import java.util.ArrayList;
import java.util.Collections;
import java.util.Iterator;
import java.util.List;

/**
 * Please add Description Here.
 */
final class IterableValidator<T, I extends Iterable<T>> implements Validator<I> {

    private final Validator<T> validator;

    IterableValidator(Validator<T> validator) {
        this.validator = validator;
    }

    @NotNull
    public List<Violation> validate(I iterable, @NotNull ValidationContext context) {
        if (iterable != null) {
            final List<Violation> violations = new ArrayList<>();
            final Iterator<T> iterator = iterable.iterator();
            int i = 0;

            while (iterator.hasNext()) {
                final T t = iterator.next();
                violations.addAll(validator.validate(t, context.addPath("[" + i++ + "]")));
            }

            return violations;
        }

        return Collections.emptyList();
    }
}
