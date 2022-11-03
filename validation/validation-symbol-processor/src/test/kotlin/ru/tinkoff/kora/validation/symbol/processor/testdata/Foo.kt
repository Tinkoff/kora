package ru.tinkoff.kora.validation.symbol.processor.testdata

import ru.tinkoff.kora.validation.annotation.NotEmpty
import ru.tinkoff.kora.validation.annotation.Pattern
import ru.tinkoff.kora.validation.annotation.Range
import ru.tinkoff.kora.validation.annotation.Validated
import java.time.OffsetDateTime

@Validated
data class Foo(
    @NotEmpty @Pattern("\\d+")
    val number: String,
    @Range(from = 1.0, to = 10.0)
    val code: Long,
    val timestamp: OffsetDateTime,
    @Validated
    val bar: Bar?
)
