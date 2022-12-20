package ru.tinkoff.kora.test.extension.junit5;

import com.typesafe.config.Config;
import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.Test;

@KoraAppTest(
    application = TestApplication.class,
    classes = {TestFirstComponent.class, TestSecondComponent.class},
    config = """
        resilient {
          circuitBreaker {
            fast {
              default {
                slidingWindowSize = 1
                minimumRequiredCalls = 1
                failureRateThreshold = 100
                permittedCallsInHalfOpenState = 1
                waitDurationInOpenState = 1s
              }
            }
          }
        }
        """)
public class JUnitExtensionTests extends Assertions {

    @Test
    void emptyTest() {
        // do nothing
    }

    @Test
    void configGeneratedForApplication(Config config) {
        assertEquals("Config(SimpleConfigObject({\"resilient\":{\"circuitBreaker\":{\"fast\":{\"default\":{\"failureRateThreshold\":100,\"minimumRequiredCalls\":1,\"permittedCallsInHalfOpenState\":1,\"slidingWindowSize\":1,\"waitDurationInOpenState\":\"1s\"}}}}}))",
            config.toString());
    }

    @Test
    void singleComponentInjected(TestFirstComponent firstComponent) {
        assertEquals("1", firstComponent.get());
    }

    @Test
    void twoComponentsInjected(TestFirstComponent firstComponent, TestSecondComponent secondComponent) {
        assertEquals("1", firstComponent.get());
        assertEquals("1", secondComponent.get());
    }
}
