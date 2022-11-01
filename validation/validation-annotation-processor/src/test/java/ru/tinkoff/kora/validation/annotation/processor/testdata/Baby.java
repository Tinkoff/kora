package ru.tinkoff.kora.validation.annotation.processor.testdata;

import ru.tinkoff.kora.validation.annotation.NotEmpty;
import ru.tinkoff.kora.validation.annotation.Pattern;
import ru.tinkoff.kora.validation.annotation.Size;
import ru.tinkoff.kora.validation.annotation.Validated;

import javax.annotation.Nullable;
import java.time.OffsetDateTime;

@Validated
public record Baby(@NotEmpty @Pattern("\\d+") String number,
                   @Size(min = 1, max = 10) Long code,
                   @Nullable OffsetDateTime timestamp,
                   @Validated Yoda yoda) {}
