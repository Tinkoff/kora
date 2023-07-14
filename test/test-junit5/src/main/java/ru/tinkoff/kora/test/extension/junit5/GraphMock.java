package ru.tinkoff.kora.test.extension.junit5;

import org.mockito.Mockito;
import reactor.core.publisher.Mono;
import ru.tinkoff.kora.application.graph.ApplicationGraphDraw;
import ru.tinkoff.kora.application.graph.Lifecycle;

record GraphMock(GraphCandidate candidate) implements GraphModification {

    @Override
    public void accept(ApplicationGraphDraw graphDraw) {
        var nodesToMock = GraphUtils.findNodeByTypeOrAssignable(graphDraw, candidate());
        if (nodesToMock.isEmpty()) {
            throw new IllegalArgumentException("Can't mock component %s because it is not present in graph".formatted(candidate.toString()));
        }
        for (var nodeToMock : nodesToMock) {
            graphDraw.replaceNode(nodeToMock, g -> {
                if (candidate().type() instanceof Class<?> mockClass) {
                    var mock = Mockito.mock(mockClass);
                    if (Lifecycle.class.isAssignableFrom(mockClass)) {
                        Mockito.when(((Lifecycle) mock).init()).thenReturn(Mono.empty());
                        Mockito.when(((Lifecycle) mock).release()).thenReturn(Mono.empty());
                    }
                    return mock;
                } else {
                    throw new IllegalArgumentException("Can't mock type: " + candidate());
                }
            });
        }
    }
}
