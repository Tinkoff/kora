package ru.tinkoff.kora.validation.annotation.processor;

import org.junit.jupiter.api.Test;
import ru.tinkoff.kora.validation.annotation.processor.testdata.ValidTaz;

class ValidateAspectTests extends ValidateRunner {

    @Test
    void validSyncInputSuccess() {
        // given
        var service = getValidateSync();

        // then
        service.validatedInput(1, "1", new ValidTaz("1"));
    }
}
