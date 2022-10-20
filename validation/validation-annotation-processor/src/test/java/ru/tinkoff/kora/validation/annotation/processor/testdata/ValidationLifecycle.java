package ru.tinkoff.kora.validation.annotation.processor.testdata;

import ru.tinkoff.kora.annotation.processor.common.MockLifecycle;
import ru.tinkoff.kora.validation.example.BabyValidator;
import ru.tinkoff.kora.validation.example.YodaValidator;

/**
 * Please add Description Here.
 */
public record ValidationLifecycle(BabyValidator babyValidator, YodaValidator yodaValidator) implements MockLifecycle {
}
