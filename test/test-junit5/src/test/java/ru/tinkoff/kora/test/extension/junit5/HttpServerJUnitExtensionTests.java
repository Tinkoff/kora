package ru.tinkoff.kora.test.extension.junit5;

import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.Test;
import ru.tinkoff.kora.config.annotation.processor.processor.ConfigRootAnnotationProcessor;
import ru.tinkoff.kora.config.annotation.processor.processor.ConfigSourceAnnotationProcessor;
import ru.tinkoff.kora.test.extension.junit5.testdata.HttpServerApplication;
import ru.tinkoff.kora.test.extension.junit5.testdata.SimpleComponent1;

@KoraAppTest(
    application = HttpServerApplication.class,
    components = {SimpleComponent1.class},
    processors = {
        ConfigRootAnnotationProcessor.class,
        ConfigSourceAnnotationProcessor.class,
    },
    config = """
        httpServer {
          publicApiHttpPort = 8091
          privateApiHttpPort = 8096
        }
        """)
class HttpServerJUnitExtensionTests extends Assertions {

    @Test
    void componentInjected(@TestComponent SimpleComponent1 component1) {
        assertEquals("1", component1.get());
    }
}
