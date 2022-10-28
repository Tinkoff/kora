package ru.tinkoff.kora.validation.annotation.processor.testdata;

import ru.tinkoff.kora.validation.annotation.NotEmpty;
import ru.tinkoff.kora.validation.annotation.Range;
import ru.tinkoff.kora.validation.annotation.Validated;

import java.time.OffsetDateTime;

@Validated
public record Baby(@NotEmpty String number, @Range(from = 1, to = 10) Long code, OffsetDateTime timestamp, Yoda yoda) {
}
