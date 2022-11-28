package ru.tinkoff.kora.resilient.annotation.processor.aop;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.TestInstance;
import ru.tinkoff.kora.resilient.annotation.processor.aop.testdata.TimeoutTarget;
import ru.tinkoff.kora.resilient.timeout.TimeoutException;

@TestInstance(TestInstance.Lifecycle.PER_CLASS)
class TimeoutTests extends TimeoutRunner {

    private TimeoutTarget getService(InitializedGraph graph) {
        var values = graph.graphDraw().getNodes()
            .stream()
            .map(graph.refreshableGraph()::get)
            .toList();

        return values.stream()
            .filter(a -> a instanceof TimeoutTarget)
            .map(a -> ((TimeoutTarget) a))
            .findFirst().orElseThrow();
    }

    @Test
    void syncTimeout() {
        // given
        var graphDraw = createGraphDraw();
        var service = getService(graphDraw);

        assertThrows(TimeoutException.class, service::getValueSync);
    }

    @Test
    void monoTimeout() {
        // given
        var graphDraw = createGraphDraw();
        var service = getService(graphDraw);

        // then
        assertThrows(RuntimeException.class, () -> service.getValueMono().block());
    }

    @Test
    void fluxTimeout() {
        // given
        var graphDraw = createGraphDraw();
        var service = getService(graphDraw);

        // when
        assertThrows(RuntimeException.class, () -> service.getValueFlux().blockFirst());
    }
}
