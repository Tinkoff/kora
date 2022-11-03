package ru.tinkoff.kora.validation.annotation.processor.testdata;

import ru.tinkoff.kora.validation.annotation.NotEmpty;
import ru.tinkoff.kora.validation.annotation.Pattern;
import ru.tinkoff.kora.validation.annotation.Range;
import ru.tinkoff.kora.validation.annotation.Validated;

import javax.annotation.Nullable;
import java.time.OffsetDateTime;

@Validated
public record Foo(@NotEmpty @Pattern("\\d+") String number,
                  @Range(from = 1, to = 10) Long code,
                  @Nullable OffsetDateTime timestamp,
                  @Validated @Nullable Bar bar) {}
