package ru.tinkoff.kora.validation.symbol.processor

import com.google.devtools.ksp.KspExperimental
import com.squareup.kotlinpoet.ksp.KotlinPoetKspPreview
import org.junit.jupiter.api.Test
import org.junit.jupiter.api.TestInstance
import ru.tinkoff.kora.validation.ValidationContext
import ru.tinkoff.kora.validation.symbol.processor.testdata.Bar
import ru.tinkoff.kora.validation.symbol.processor.testdata.Foo
import java.time.OffsetDateTime

@TestInstance(TestInstance.Lifecycle.PER_CLASS)
@KspExperimental
@KotlinPoetKspPreview
class ValidationSymbolProcessorTests : TestAppRunner() {

    @Test
    fun validateSuccess() {
        // give
        val lifecycle = getService()
        val bar = Bar()
        bar.id = "1"
        bar.codes = listOf(1)
        bar.babies = listOf(Foo("1", 1, OffsetDateTime.now(), null))

        // then
        val violations = lifecycle.bar.validate(bar);
        assertTrue(violations.isEmpty())
    }

    @Test
    fun validateRangeFail() {
        // give
        val lifecycle = getService()
        val value = Foo("1", 0, OffsetDateTime.now(), null)

        // then
        val violations = lifecycle.foo.validate(value);
        assertEquals(1, violations.size)
    }

    @Test
    fun validateInnerValidatorForListFail() {
        // give
        val lifecycle = getService()
        val value = Bar()
        value.id = "1"
        value.codes = listOf(1)
        value.babies = listOf(Foo("1", 0, OffsetDateTime.now(), null))

        // then
        val violations = lifecycle.bar.validate(value);
        assertEquals(1, violations.size)
    }

    @Test
    fun validateInnerValidatorForValueFail() {
        // give
        val lifecycle = getService()
        val bar = Bar().apply {
            id = "1"
            codes = listOf()
        }
        val value = Foo("1", 1, OffsetDateTime.now(), bar)

        // then
        val violations = lifecycle.foo.validate(value);
        assertEquals(1, violations.size)
    }

    @Test
    fun validateFailFast() {
        // give
        val lifecycle = getService()
        val bar = Bar().apply {
            id = "1"
            codes = listOf(1, 2, 3, 4, 5, 6)
        }
        val value = Foo("1", 0, OffsetDateTime.now(), bar)

        // then
        val violations = lifecycle.foo.validate(value, ValidationContext.builder().failFast(true).build())
        assertEquals(1, violations.size)
    }

    @Test
    fun validateFailSlow() {
        // give
        val lifecycle = getService()
        val bar = Bar().apply {
            id = "1"
            codes = listOf(1, 2, 3, 4, 5, 6)
        }
        val value = Foo("1", 0, OffsetDateTime.now(), bar)

        // then
        val violations = lifecycle.foo.validate(value, ValidationContext.builder().failFast(false).build())
        assertEquals(2, violations.size)
    }
}
