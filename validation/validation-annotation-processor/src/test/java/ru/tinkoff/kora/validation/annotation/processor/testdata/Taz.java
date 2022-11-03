package ru.tinkoff.kora.validation.annotation.processor.testdata;

import ru.tinkoff.kora.validation.annotation.Pattern;
import ru.tinkoff.kora.validation.annotation.Validated;

@Validated
public record Taz(@Pattern("\\d+") String number) {}
