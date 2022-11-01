package ru.tinkoff.kora.validation.annotation.processor.testdata;

import ru.tinkoff.kora.validation.annotation.NotEmpty;
import ru.tinkoff.kora.validation.annotation.Pattern;
import ru.tinkoff.kora.validation.annotation.Size;
import ru.tinkoff.kora.validation.annotation.Validated;

import javax.annotation.Nullable;
import java.time.OffsetDateTime;

@Validated
public record Baby(@NotEmpty @Pattern("\\d+") String number,
                   @Size(from = 1, to = 10) Long code,
                   @Nullable OffsetDateTime timestamp,
                   Yoda yoda) {
}
