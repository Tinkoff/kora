package ru.tinkoff.kora.validation.common.constraint;

import org.jetbrains.annotations.NotNull;
import ru.tinkoff.kora.validation.common.ValidationContext;
import ru.tinkoff.kora.validation.common.Validator;
import ru.tinkoff.kora.validation.common.Violation;
import ru.tinkoff.kora.validation.common.annotation.Range;

import java.math.BigDecimal;
import java.util.Collections;
import java.util.List;
import java.util.function.Predicate;

final class RangeLongNumberValidator<T extends Number> implements Validator<T> {

    private final long from;
    private final long to;
    private final Range.Boundary boundary;

    private final Predicate<T> fromPredicate;
    private final Predicate<T> toPredicate;

    RangeLongNumberValidator(double fromDouble, double toDouble, Range.Boundary boundary) {
        this.from = BigDecimal.valueOf(fromDouble).longValue();
        this.to = BigDecimal.valueOf(toDouble).longValue();
        this.boundary = boundary;
        this.fromPredicate = switch (boundary) {
            case INCLUSIVE_INCLUSIVE, INCLUSIVE_EXCLUSIVE -> (v -> v.longValue() >= from);
            case EXCLUSIVE_INCLUSIVE, EXCLUSIVE_EXCLUSIVE -> (v -> v.longValue() > from);
        };

        this.toPredicate = switch (boundary) {
            case INCLUSIVE_EXCLUSIVE, EXCLUSIVE_EXCLUSIVE -> (v -> v.longValue() < to);
            case EXCLUSIVE_INCLUSIVE, INCLUSIVE_INCLUSIVE -> (v -> v.longValue() <= to);
        };
    }

    @NotNull
    @Override
    public List<Violation> validate(T value, @NotNull ValidationContext context) {
        if (value == null) {
            return List.of(context.violates("Should be in range from '" + from + "' to '" + to + "', but was null"));
        }

        if (!fromPredicate.test(value)) {
            return List.of(context.violates("Should be in range from '" + from + "' to '" + to + "', but was smaller: " + value));
        } else if (!toPredicate.test(value)) {
            return List.of(context.violates("Should be in range from '" + from + "' to '" + to + "', but was greater: " + value));
        }

        return Collections.emptyList();
    }
}
