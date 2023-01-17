package ru.tinkoff.kora.resilient.annotation.processor.aop;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.TestInstance;
import ru.tinkoff.kora.resilient.annotation.processor.aop.testdata.RetryableTarget;
import ru.tinkoff.kora.resilient.retry.RetryAttemptException;

import java.time.Duration;

@TestInstance(TestInstance.Lifecycle.PER_CLASS)
class RetryableTests extends TestRunner {

    private RetryableTarget getService(InitializedGraph graph) {
        var values = graph.graphDraw().getNodes()
            .stream()
            .map(graph.refreshableGraph()::get)
            .toList();

        return values.stream()
            .filter(a -> a instanceof RetryableTarget)
            .map(a -> ((RetryableTarget) a))
            .findFirst().orElseThrow();
    }

    private static final int RETRY_SUCCESS = 1;
    private static final int RETRY_FAIL = 5;

    private final RetryableTarget retryableTarget = getService(createGraphDraw());

    @BeforeEach
    void setup() {
        retryableTarget.reset();
    }

    @Test
    void syncVoidRetrySuccess() {
        // given
        var service = retryableTarget;

        // then
        service.setRetryAttempts(RETRY_SUCCESS);

        // then
        service.retrySyncVoid("1");
    }

    @Test
    void syncVoidRetryFail() {
        // given
        var service = retryableTarget;

        // then
        service.setRetryAttempts(RETRY_FAIL);

        // then
        try {
            service.retrySyncVoid("1");
            fail("Should not happen");
        } catch (RetryAttemptException ex) {
            assertNotNull(ex.getMessage());
        }
    }

    @Test
    void syncRetrySuccess() {
        // given
        var service = retryableTarget;

        // then
        service.setRetryAttempts(RETRY_SUCCESS);

        // then
        assertEquals("1", service.retrySync("1"));
    }

    @Test
    void syncRetryFail() {
        // given
        var service = retryableTarget;

        // then
        service.setRetryAttempts(RETRY_FAIL);

        // then
        try {
            service.retrySync("1");
            fail("Should not happen");
        } catch (RetryAttemptException ex) {
            assertNotNull(ex.getMessage());
        }
    }

    @Test
    void monoRetrySuccess() {
        // given
        var service = retryableTarget;

        // then
        service.setRetryAttempts(RETRY_SUCCESS);

        // then
        assertEquals("1", service.retryMono("1").block(Duration.ofMinutes(1)));
    }

    @Test
    void monoRetryFail() {
        // given
        var service = retryableTarget;

        // then
        service.setRetryAttempts(RETRY_FAIL);

        // then
        try {
            service.retryMono("1").block(Duration.ofMinutes(1));
            fail("Should not happen");
        } catch (RetryAttemptException e) {
            assertNotNull(e.getMessage());
        }
    }

    @Test
    void fluxRetrySuccess() {
        // given
        var service = retryableTarget;

        // then
        service.setRetryAttempts(RETRY_SUCCESS);

        // then
        assertEquals("1", service.retryFlux("1").blockFirst(Duration.ofMinutes(1)));
    }

    @Test
    void fluxRetryFail() {
        // given
        var service = retryableTarget;

        // then
        service.setRetryAttempts(RETRY_FAIL);

        // then
        try {
            service.retryFlux("1").blockFirst(Duration.ofMinutes(1));
            fail("Should not happen");
        } catch (RetryAttemptException e) {
            assertNotNull(e.getMessage());
        }
    }
}
