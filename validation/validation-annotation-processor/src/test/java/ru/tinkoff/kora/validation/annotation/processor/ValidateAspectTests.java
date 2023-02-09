package ru.tinkoff.kora.validation.annotation.processor;

import org.junit.jupiter.api.Test;
import ru.tinkoff.kora.validation.annotation.processor.testdata.ValidTaz;
import ru.tinkoff.kora.validation.common.ViolationException;

class ValidateAspectTests extends ValidateRunner {

    @Test
    void validateInputSyncSuccess() {
        // given
        var service = getValidateSync();

        // then
        assertDoesNotThrow(() -> service.validatedInput(1, "1", new ValidTaz("1")));
    }

    @Test
    void validateInputSyncFails() {
        // given
        var service = getValidateSync();

        // then
        assertThrows(ViolationException.class, () -> service.validatedInput(0, "1", new ValidTaz("1")));
        assertThrows(ViolationException.class, () -> service.validatedInput(1, "", new ValidTaz("1")));
        assertThrows(ViolationException.class, () -> service.validatedInput(1, "1", new ValidTaz("A")));
        var allViolations = assertThrows(ViolationException.class, () -> service.validatedInput(0, "", new ValidTaz("A")));
        assertEquals(3, allViolations.getViolations().size());
    }

    @Test
    void validateOutputSyncSuccess() {
        // given
        var service = getValidateSync();

        // then
        assertDoesNotThrow(() -> service.validatedOutput(new ValidTaz("1"), null));
    }

    @Test
    void validateOutputSyncFails() {
        // given
        var service = getValidateSync();

        // then
        assertThrows(ViolationException.class, () -> service.validatedOutput(new ValidTaz("A"), null));
        assertThrows(ViolationException.class, () -> service.validatedOutput(new ValidTaz("1"), new ValidTaz("1")));
    }

    @Test
    void validateInputMonoSuccess() {
        // given
        var service = getValidateMono();

        // then
        assertDoesNotThrow(() -> service.validatedInput(1, "1", new ValidTaz("1")).block());
    }

    @Test
    void validateInputMonoFails() {
        // given
        var service = getValidateMono();

        // then
        assertThrows(ViolationException.class, () -> service.validatedInput(0, "1", new ValidTaz("1")).block());
        assertThrows(ViolationException.class, () -> service.validatedInput(1, "", new ValidTaz("1")).block());
        assertThrows(ViolationException.class, () -> service.validatedInput(1, "1", new ValidTaz("A")).block());
        var allViolations = assertThrows(ViolationException.class, () -> service.validatedInput(0, "", new ValidTaz("A")).block());
        assertEquals(3, allViolations.getViolations().size());
    }


    @Test
    void validateOutputMonoSuccess() {
        // given
        var service = getValidateMono();

        // then
        assertDoesNotThrow(() -> service.validatedOutput(new ValidTaz("1"), null).block());
    }

    @Test
    void validateOutputMonoFails() {
        // given
        var service = getValidateMono();

        // then
        assertThrows(ViolationException.class, () -> service.validatedOutput(new ValidTaz("A"), null).block());
        assertThrows(ViolationException.class, () -> service.validatedOutput(new ValidTaz("1"), new ValidTaz("1")).block());
    }

    @Test
    void validateInputFluxSuccess() {
        // given
        var service = getValidateFlux();

        // then
        assertDoesNotThrow(() -> service.validatedInput(1, "1", new ValidTaz("1")).collectList().block());
    }

    @Test
    void validateInputFluxFails() {
        // given
        var service = getValidateFlux();

        // then
        assertThrows(ViolationException.class, () -> service.validatedInput(0, "1", new ValidTaz("1")).collectList().block());
        assertThrows(ViolationException.class, () -> service.validatedInput(1, "", new ValidTaz("1")).collectList().block());
        assertThrows(ViolationException.class, () -> service.validatedInput(1, "1", new ValidTaz("A")).collectList().block());
        var allViolations = assertThrows(ViolationException.class, () -> service.validatedInput(0, "", new ValidTaz("A")).collectList().block());
        assertEquals(3, allViolations.getViolations().size());
    }


    @Test
    void validateOutputFluxSuccess() {
        // given
        var service = getValidateFlux();

        // then
        assertDoesNotThrow(() -> service.validatedOutput(new ValidTaz("1"), null).collectList().block());
    }

    @Test
    void validateOutputFluxFails() {
        // given
        var service = getValidateFlux();

        // then
        assertThrows(ViolationException.class, () -> service.validatedOutput(new ValidTaz("A"), null).collectList().block());
        assertThrows(ViolationException.class, () -> service.validatedOutput(new ValidTaz("1"), new ValidTaz("1")).collectList().block());
    }
}
