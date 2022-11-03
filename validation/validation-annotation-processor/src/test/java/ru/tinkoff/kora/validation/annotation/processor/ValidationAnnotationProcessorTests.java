package ru.tinkoff.kora.validation.annotation.processor;

import org.junit.jupiter.api.Test;
import ru.tinkoff.kora.validation.ValidationContext;
import ru.tinkoff.kora.validation.Violation;
import ru.tinkoff.kora.validation.annotation.processor.testdata.Bar;
import ru.tinkoff.kora.validation.annotation.processor.testdata.Foo;
import ru.tinkoff.kora.validation.annotation.processor.testdata.ValidationLifecycle;

import java.time.OffsetDateTime;
import java.util.List;

class ValidationAnnotationProcessorTests extends TestAppRunner {

    @Test
    void validateSuccess() {
        // given
        final ValidationLifecycle lifecycle = getService(ValidationLifecycle.class);
        final Bar value = new Bar()
            .setId("1")
            .setCodes(List.of(1))
            .setFoos(List.of(
                new Foo("1", 1L, OffsetDateTime.now(), null)
            ));

        // then
        final List<Violation> violations = lifecycle.bar().validate(value);
        assertTrue(violations.isEmpty());
    }

    @Test
    void validateRangeFail() {
        // given
        final ValidationLifecycle lifecycle = getService(ValidationLifecycle.class);
        var value = new Foo("1", 1111L, OffsetDateTime.now(), null);

        // then
        final List<Violation> violations = lifecycle.foo().validate(value);
        assertEquals(1, violations.size());
    }

    @Test
    void validateInnerValidatorForListFail() {
        // given
        final ValidationLifecycle lifecycle = getService(ValidationLifecycle.class);
        var value = new Bar()
            .setId("1")
            .setCodes(List.of(1))
            .setFoos(List.of(
                new Foo("1", 0L, OffsetDateTime.now(), null)
            ));

        // then
        final List<Violation> violations = lifecycle.bar().validate(value);
        assertEquals(1, violations.size());
    }

    @Test
    void validateInnerValidatorForValueFail() {
        // given
        final ValidationLifecycle lifecycle = getService(ValidationLifecycle.class);
        var value = new Foo("1", 1L, OffsetDateTime.now(), new Bar()
            .setId("1")
            .setCodes(List.of(1, 2, 3, 4, 5, 6)));

        // then
        final List<Violation> violations = lifecycle.foo().validate(value);
        assertEquals(1, violations.size());
    }

    @Test
    void validateFailFast() {
        // given
        final ValidationLifecycle lifecycle = getService(ValidationLifecycle.class);
        var value = new Foo("1", 0L, OffsetDateTime.now(), new Bar()
            .setId("1")
            .setCodes(List.of(1, 2, 3, 4, 5, 6)));

        // then
        final List<Violation> violations = lifecycle.foo().validate(value, ValidationContext.builder().failFast(true).build());
        assertEquals(1, violations.size());
    }

    @Test
    void validateFailSlow() {
        // given
        final ValidationLifecycle lifecycle = getService(ValidationLifecycle.class);
        var value = new Foo("1", 0L, OffsetDateTime.now(), new Bar()
            .setId("1")
            .setCodes(List.of(1, 2, 3, 4, 5, 6)));

        // then
        final List<Violation> violations = lifecycle.foo().validate(value, ValidationContext.builder().failFast(false).build());
        assertEquals(2, violations.size());
    }
}
