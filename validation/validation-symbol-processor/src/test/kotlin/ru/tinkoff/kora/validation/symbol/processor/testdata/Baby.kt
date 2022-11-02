package ru.tinkoff.kora.validation.symbol.processor.testdata

import ru.tinkoff.kora.validation.annotation.*
import java.time.OffsetDateTime
import javax.annotation.Nullable

@Validated
data class Baby(
    @NotEmpty @Pattern("\\d+")
    val number: String,
    @Range(from = 1.0, to = 10.0)
    val code: Long,
    val timestamp: OffsetDateTime,
    @Validated
    val yoda: Yoda?
)
