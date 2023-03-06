package ru.tinkoff.kora.test.extension.junit5;

import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.Test;
import ru.tinkoff.kora.config.annotation.processor.processor.ConfigRootAnnotationProcessor;
import ru.tinkoff.kora.config.annotation.processor.processor.ConfigSourceAnnotationProcessor;
import ru.tinkoff.kora.test.extension.junit5.testdata.SimpleApplication;
import ru.tinkoff.kora.test.extension.junit5.testdata.SimpleComponent1;
import ru.tinkoff.kora.test.extension.junit5.testdata.SimpleComponent2;

@KoraAppTest(
    application = SimpleApplication.class,
    components = {SimpleComponent1.class, SimpleComponent2.class},
    processors = {
        ConfigRootAnnotationProcessor.class,
        ConfigSourceAnnotationProcessor.class
    })
public class ComponentJUnitExtensionTests extends Assertions {

    @Test
    void emptyTest() {
        // do nothing
    }

    @Test
    void parameterFirstInjected(@TestComponent SimpleComponent1 firstComponent) {
        assertEquals("1", firstComponent.get());
    }

    @Test
    void parameterBothInjected(@TestComponent SimpleComponent1 firstComponent,
                               @TestComponent SimpleComponent2 secondComponent) {
        assertEquals("1", firstComponent.get());
        assertEquals("12", secondComponent.get());
    }
}
