package ru.tinkoff.kora.validation.common.constraint;

import org.jetbrains.annotations.NotNull;
import ru.tinkoff.kora.validation.common.ValidationContext;
import ru.tinkoff.kora.validation.common.Validator;
import ru.tinkoff.kora.validation.common.Violation;
import ru.tinkoff.kora.validation.common.annotation.Range;

import java.math.BigDecimal;
import java.math.BigInteger;
import java.util.Collections;
import java.util.List;
import java.util.function.Predicate;

final class RangeBigIntegerValidator implements Validator<BigInteger> {

    private final BigInteger from;
    private final BigInteger to;
    private final Range.Boundary boundary;

    private final Predicate<BigInteger> fromPredicate;
    private final Predicate<BigInteger> toPredicate;

    RangeBigIntegerValidator(double fromDouble, double toDouble, Range.Boundary boundary) {
        this.from = BigDecimal.valueOf(fromDouble).toBigInteger();
        this.to = BigDecimal.valueOf(toDouble).toBigInteger();
        this.boundary = boundary;
        this.fromPredicate = switch (boundary) {
            case INCLUSIVE_INCLUSIVE, INCLUSIVE_EXCLUSIVE -> (v -> v.compareTo(from) >= 0);
            case EXCLUSIVE_INCLUSIVE, EXCLUSIVE_EXCLUSIVE -> (v -> v.compareTo(from) > 0);
        };

        this.toPredicate = switch (boundary) {
            case INCLUSIVE_EXCLUSIVE, EXCLUSIVE_EXCLUSIVE -> (v -> v.compareTo(to) < 0);
            case EXCLUSIVE_INCLUSIVE, INCLUSIVE_INCLUSIVE -> (v -> v.compareTo(to) <= 0);
        };
    }

    @NotNull
    @Override
    public List<Violation> validate(BigInteger value, @NotNull ValidationContext context) {
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
