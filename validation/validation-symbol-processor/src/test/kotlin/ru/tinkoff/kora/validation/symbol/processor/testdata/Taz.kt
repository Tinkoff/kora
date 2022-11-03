package ru.tinkoff.kora.validation.symbol.processor.testdata

import ru.tinkoff.kora.validation.annotation.Pattern
import ru.tinkoff.kora.validation.annotation.Validated

@Validated
data class Taz(@Pattern("\\d+") val number: String)
