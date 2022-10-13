package ru.tinkoff.kora.resilient.validation.example;

import ru.tinkoff.kora.resilient.validation.annotation.NotEmpty;
import ru.tinkoff.kora.resilient.validation.annotation.NotNull;
import ru.tinkoff.kora.resilient.validation.annotation.Validated;

import java.time.OffsetDateTime;

/**
 * Please add Description Here.
 */
@Validated
public record Baby(@NotNull @NotEmpty String number, OffsetDateTime timestamp, Yoda yoda) {
}
