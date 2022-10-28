package ru.tinkoff.kora.validation.annotation.processor.testdata;

import ru.tinkoff.kora.annotation.processor.common.MockLifecycle;
import ru.tinkoff.kora.validation.Validator;

public record ValidationLifecycle(Validator<Baby> babyValidator, Validator<Yoda> yodaValidator) implements MockLifecycle {}
