package ru.tinkoff.kora.test.extension.junit5;

import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.Test;
import ru.tinkoff.kora.test.extension.junit5.testdata.Component1;
import ru.tinkoff.kora.test.extension.junit5.testdata.LifecycleApplication;
import ru.tinkoff.kora.test.extension.junit5.testdata.LifecycleComponent12;
import ru.tinkoff.kora.test.extension.junit5.testdata.LifecycleComponent2;

@KoraAppTest(application = LifecycleApplication.class)
public class ComponentAnnotationJUnitExtensionTests extends Assertions {

    @Test
    void emptyTest() {
        // do nothing
    }

    @Test
    void parameterFirstInjected(@TestComponent Component1 firstComponent) {
        assertEquals("1", firstComponent.get());
    }

    @Test
    void parameterBothInjected(@TestComponent Component1 firstComponent,
                               @TestComponent LifecycleComponent12 secondComponent) {
        assertEquals("1", firstComponent.get());
        assertEquals("12", secondComponent.get());
    }
}
