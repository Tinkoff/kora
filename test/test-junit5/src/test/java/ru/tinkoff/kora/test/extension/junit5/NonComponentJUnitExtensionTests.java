package ru.tinkoff.kora.test.extension.junit5;

import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.Test;
import ru.tinkoff.kora.config.annotation.processor.processor.ConfigRootAnnotationProcessor;
import ru.tinkoff.kora.config.annotation.processor.processor.ConfigSourceAnnotationProcessor;
import ru.tinkoff.kora.http.client.annotation.processor.HttpClientAnnotationProcessor;

@KoraAppTest(application = HttpClientApplication.class,
    components = {HttpClientImpl.class},
    processors = {
        HttpClientAnnotationProcessor.class,
        ConfigRootAnnotationProcessor.class,
        ConfigSourceAnnotationProcessor.class},
    config = """
        httpClient.default {
          url = "http://mockserver:1080"
        }
        """
)
class NonComponentJUnitExtensionTests extends Assertions {

    @Test
    void nonWorkingTest(HttpClientImpl httpClient) {
        assertNotNull(httpClient);
    }
}
