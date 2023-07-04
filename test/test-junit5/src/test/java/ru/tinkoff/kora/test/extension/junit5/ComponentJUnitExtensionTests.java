package ru.tinkoff.kora.test.extension.junit5;

import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.Test;
import ru.tinkoff.kora.test.extension.junit5.testdata.TestApplication;
import ru.tinkoff.kora.test.extension.junit5.testdata.TestComponent1;
import ru.tinkoff.kora.test.extension.junit5.testdata.TestComponent12;
import ru.tinkoff.kora.test.extension.junit5.testdata.TestComponent2;

import static org.junit.jupiter.api.Assertions.assertEquals;

@KoraAppTest(
    value = TestApplication.class,
    components = {TestComponent2.class, TestComponent12.class, TestComponent1.class})
public class ComponentJUnitExtensionTests {

    @Test
    void emptyTest() {
        // do nothing
    }

    @Test
    void parameterFirstInjected(@TestComponent TestComponent1 firstComponent) {
        assertEquals("1", firstComponent.get());
    }

    @Test
    void parameterBothInjected(@TestComponent TestComponent1 firstComponent,
                               @TestComponent TestComponent12 secondComponent) {
        assertEquals("1", firstComponent.get());
        assertEquals("12", secondComponent.get());
    }
}
