package ru.tinkoff.kora.test.extension.junit5;

import com.typesafe.config.Config;
import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.Test;
import ru.tinkoff.kora.config.annotation.processor.processor.ConfigRootAnnotationProcessor;
import ru.tinkoff.kora.http.client.annotation.processor.HttpClientAnnotationProcessor;

@KoraAppTest(
    application = SimpleApplication.class,
    components = { SimpleFirstComponent.class, SimpleSecondComponent.class },
    processors = {
        HttpClientAnnotationProcessor.class,
        ConfigRootAnnotationProcessor.class
    },
    config = """
        myconfig {
          myinnerconfig {
            myproperty = 1
          }
        }
        """)
public class ComponentJUnitExtensionTests extends Assertions {

    @Test
    void emptyTest() {
        // do nothing
    }

    @Test
    void configGeneratedForApplication(Config config) {
        assertEquals("Config(SimpleConfigObject({\"myconfig\":{\"myinnerconfig\":{\"myproperty\":1}}}))", config.toString());
    }

    @Test
    void singleComponentInjected(SimpleFirstComponent firstComponent) {
        assertEquals("1", firstComponent.get());
    }

    @Test
    void twoComponentsInjected(SimpleFirstComponent firstComponent, SimpleSecondComponent secondComponent) {
        assertEquals("1", firstComponent.get());
        assertEquals("1", secondComponent.get());
    }
}
