package ru.tinkoff.kora.example;

import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.Test;
import ru.tinkoff.kora.config.annotation.processor.processor.ConfigRootAnnotationProcessor;
import ru.tinkoff.kora.config.annotation.processor.processor.ConfigSourceAnnotationProcessor;
import ru.tinkoff.kora.http.client.annotation.processor.HttpClientAnnotationProcessor;
import ru.tinkoff.kora.test.extension.junit5.KoraAppTest;

@KoraAppTest(application = TestHttpClientApplication.class,
    classes = {TestHttpClient.class},
    processors = {
        HttpClientAnnotationProcessor.class,
        ConfigRootAnnotationProcessor.class,
        ConfigSourceAnnotationProcessor.class},
    config = """
        httpClient.default {
          url = "http://mockserver:1080"
          requestTimeout = 10s
          tracingEnabled = false
          loggingEnabled = true
          getValuesConfig {
            requestTimeout = 20s
          }
        }
        """
)
class NonWorkingTests extends Assertions {

    @Test
    void nonWorkingTest(TestHttpClient httpClient) {
        assertNotNull(httpClient);
    }
}
