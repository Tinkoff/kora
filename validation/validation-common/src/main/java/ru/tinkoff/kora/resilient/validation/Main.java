package ru.tinkoff.kora.resilient.validation;

import ru.tinkoff.kora.resilient.validation.constraint.NotEmptyIterableConstraint;
import ru.tinkoff.kora.resilient.validation.constraint.NotEmptyStringConstraint;
import ru.tinkoff.kora.resilient.validation.constraint.NotNullConstraint;
import ru.tinkoff.kora.resilient.validation.example.Baby;
import ru.tinkoff.kora.resilient.validation.example.BabyValidator;
import ru.tinkoff.kora.resilient.validation.example.Yoda;
import ru.tinkoff.kora.resilient.validation.example.YodaValidator;

import java.time.OffsetDateTime;
import java.util.List;

/**
 * Please add Description Here.
 */
public class Main {

    public static void main(String[] args) {
        final YodaValidator yodaValidator = new YodaValidator(new NotNullConstraint(), new NotEmptyStringConstraint(), new NotEmptyIterableConstraint());
        final BabyValidator babyValidator = new BabyValidator(new NotNullConstraint(), new NotEmptyStringConstraint());
        yodaValidator.setValidator1(babyValidator); // cycle dependency
        babyValidator.setValidator1(yodaValidator); // cycle dependency

        Yoda yodaInnerInnerNull = new Yoda("1", List.of(1),
            List.of(new Baby("1", OffsetDateTime.now(),
                new Yoda("2", List.of(2),
                    List.of(new Baby("1", OffsetDateTime.now(), null),
                        new Baby(null, null, null))))));
        yodaValidator.validateAndThrow(yodaInnerInnerNull);
    }
}
