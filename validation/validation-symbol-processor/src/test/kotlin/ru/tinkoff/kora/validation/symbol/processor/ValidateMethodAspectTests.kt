package ru.tinkoff.kora.validation.symbol.processor

import com.google.devtools.ksp.KspExperimental
import org.junit.jupiter.api.Test
import org.junit.jupiter.api.TestInstance
import ru.tinkoff.kora.validation.common.ValidationContext
import ru.tinkoff.kora.validation.symbol.processor.testdata.ValidBar
import ru.tinkoff.kora.validation.symbol.processor.testdata.ValidFoo
import ru.tinkoff.kora.validation.symbol.processor.testdata.ValidTaz
import java.time.OffsetDateTime

@TestInstance(TestInstance.Lifecycle.PER_CLASS)
@KspExperimental
class ValidateMethodAspectTests : ValidateRunner() {

    @Test
    fun validateInputSyncSuccess() {
        // give
        val service = getValidateSync()

        // then
        assertDoesNotThrow { service.validatedInput(1, "1", ValidTaz("1")) }
    }
}
