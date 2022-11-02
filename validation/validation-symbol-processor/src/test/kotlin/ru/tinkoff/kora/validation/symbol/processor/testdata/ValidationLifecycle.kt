package ru.tinkoff.kora.validation.symbol.processor.testdata

import ru.tinkoff.kora.annotation.processor.common.MockLifecycle
import ru.tinkoff.kora.validation.Validator

data class ValidationLifecycle(val baby: Validator<Baby>, val yoda: Validator<Yoda>) : MockLifecycle
