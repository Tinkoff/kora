package ru.tinkoff.kora.validation.annotation.processor.testdata;

import ru.tinkoff.kora.annotation.processor.common.MockLifecycle;

/**
 * Please add Description Here.
 */
public record ValidationLifecycle(BabyValidator babyValidator, YodaValidator yodaValidator) implements MockLifecycle {
}
