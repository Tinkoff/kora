package ru.tinkoff.kora.validation.symbol.processor.testdata

import org.jetbrains.annotations.Nullable
import ru.tinkoff.kora.validation.annotation.NotEmpty
import ru.tinkoff.kora.validation.annotation.Validated

@Validated
class Yoda {

    @NotEmpty
    var id: String? = null
        get() = field
        set(value) { field = value }

    @NotEmpty
    var codes: List<Int> = emptyList()
    @Validated
    var babies: List<Baby> = emptyList()
}
