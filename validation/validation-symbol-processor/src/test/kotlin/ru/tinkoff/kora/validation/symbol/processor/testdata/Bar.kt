package ru.tinkoff.kora.validation.symbol.processor.testdata

import ru.tinkoff.kora.validation.annotation.NotEmpty
import ru.tinkoff.kora.validation.annotation.Size
import ru.tinkoff.kora.validation.annotation.Validated

@Validated
class Bar {

    @NotEmpty
    var id: String? = null
        get() = field
        set(value) {
            field = value
        }

    @Size(min = 1, max = 5)
    var codes: List<Int> = emptyList()

    @Validated
    var tazs: List<Taz> = emptyList()
}
