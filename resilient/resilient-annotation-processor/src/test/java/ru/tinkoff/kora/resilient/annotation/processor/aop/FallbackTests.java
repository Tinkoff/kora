package ru.tinkoff.kora.resilient.annotation.processor.aop;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.TestInstance;
import ru.tinkoff.kora.annotation.processor.common.TestUtils;
import ru.tinkoff.kora.aop.annotation.processor.AopAnnotationProcessor;
import ru.tinkoff.kora.resilient.annotation.processor.aop.testdata.FallbackIncorrectArgumentTarget;
import ru.tinkoff.kora.resilient.annotation.processor.aop.testdata.FallbackIncorrectSignatureTarget;
import ru.tinkoff.kora.resilient.annotation.processor.aop.testdata.FallbackTarget;

@TestInstance(TestInstance.Lifecycle.PER_CLASS)
class FallbackTests extends FallbackRunner {

    private FallbackTarget getService(InitializedGraph graph) {
        var values = graph.graphDraw().getNodes()
            .stream()
            .map(graph.refreshableGraph()::get)
            .toList();

        return values.stream()
            .filter(a -> a instanceof FallbackTarget)
            .map(a -> ((FallbackTarget) a))
            .findFirst().orElseThrow();
    }

    @Test
    void incorrectArgumentFallback() {
        assertThrows(TestUtils.CompilationErrorException.class, () -> TestUtils.annotationProcess(FallbackIncorrectArgumentTarget.class, new AopAnnotationProcessor()));
    }

    @Test
    void incorrectSignatureFallback() {
        assertThrows(TestUtils.CompilationErrorException.class, () -> TestUtils.annotationProcess(FallbackIncorrectSignatureTarget.class, new AopAnnotationProcessor()));
    }

    @Test
    void syncFallback() {
        // given
        var graphDraw = createGraphDraw();
        var service = getService(graphDraw);
        service.alwaysFail = false;

        // when
        assertEquals(FallbackTarget.VALUE, service.getValueSync());
        service.alwaysFail = true;

        // then
        assertEquals(FallbackTarget.FALLBACK, service.getValueSync());
}

    @Test
    void monoFallback() {
        // given
        var graphDraw = createGraphDraw();
        var service = getService(graphDraw);
        service.alwaysFail = false;

        // when
        assertEquals(FallbackTarget.VALUE, service.getValueMono().block());
        service.alwaysFail = true;

        // then
        assertEquals(FallbackTarget.FALLBACK, service.getValueMono().block());
    }

    @Test
    void fluxFallback() {
        // given
        var graphDraw = createGraphDraw();
        var service = getService(graphDraw);
        service.alwaysFail = false;

        // when
        assertEquals(FallbackTarget.VALUE, service.getValueFlux().blockFirst());
        service.alwaysFail = true;

        // then
        assertEquals(FallbackTarget.FALLBACK, service.getValueFlux().blockFirst());
    }
}
