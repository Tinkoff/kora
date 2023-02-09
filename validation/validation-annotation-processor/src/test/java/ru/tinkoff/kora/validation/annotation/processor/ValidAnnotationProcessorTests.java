package ru.tinkoff.kora.validation.annotation.processor;

import org.junit.jupiter.api.Test;
import ru.tinkoff.kora.validation.common.ValidationContext;
import ru.tinkoff.kora.validation.common.Violation;
import ru.tinkoff.kora.validation.annotation.processor.testdata.ValidBar;
import ru.tinkoff.kora.validation.annotation.processor.testdata.ValidFoo;
import ru.tinkoff.kora.validation.annotation.processor.testdata.ValidTaz;

import java.time.OffsetDateTime;
import java.util.List;

class ValidAnnotationProcessorTests extends ValidRunner {

    @Test
    void validateSuccess() {
        // given
        var service = getBarValidator();
        final ValidBar value = new ValidBar()
            .setId("1")
            .setCodes(List.of(1))
            .setTazs(List.of(
                new ValidTaz("1")
            ));

        // then
        final List<Violation> violations = service.validate(value);
        assertEquals(0, violations.size());
    }

    @Test
    void validateRangeFail() {
        // given
        var service = getFooValidator();
        var value = new ValidFoo("1", 1111L, OffsetDateTime.now(), null);

        // then
        final List<Violation> violations = service.validate(value);
        assertEquals(1, violations.size());
    }

    @Test
    void validateInnerValidatorForListFail() {
        // given
        var service = getBarValidator();
        var value = new ValidBar()
            .setId("1")
            .setCodes(List.of(1))
            .setTazs(List.of(
                new ValidTaz("a")
            ));

        // then
        final List<Violation> violations = service.validate(value);
        assertEquals(1, violations.size());
    }

    @Test
    void validateInnerValidatorForValueFail() {
        // given
        var service = getFooValidator();
        var value = new ValidFoo("1", 1L, OffsetDateTime.now(), new ValidBar()
            .setId("1")
            .setCodes(List.of(1, 2, 3, 4, 5, 6)));

        // then
        final List<Violation> violations = service.validate(value);
        assertEquals(1, violations.size());
    }

    @Test
    void validateFailFast() {
        // given
        var service = getFooValidator();
        var value = new ValidFoo("1", 0L, OffsetDateTime.now(), new ValidBar()
            .setId("1")
            .setCodes(List.of(1, 2, 3, 4, 5, 6)));

        // then
        final List<Violation> violations = service.validate(value, ValidationContext.builder().failFast(true).build());
        assertEquals(1, violations.size());
    }

    @Test
    void validateFailSlow() {
        // given
        var service = getFooValidator();
        var value = new ValidFoo("1", 0L, OffsetDateTime.now(),
            new ValidBar()
                .setId("1")
                .setCodes(List.of(1, 2, 3, 4, 5, 6)));

        // then
        final List<Violation> violations = service.validate(value, ValidationContext.builder().failFast(false).build());
        assertEquals(2, violations.size());
    }
}
