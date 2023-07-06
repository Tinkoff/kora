package ru.tinkoff.kora.test.extension.junit5;

import org.mockito.Mockito;
import reactor.core.publisher.Mono;
import ru.tinkoff.kora.application.graph.ApplicationGraphDraw;
import ru.tinkoff.kora.application.graph.Lifecycle;
import ru.tinkoff.kora.application.graph.Node;

import java.util.Set;

record GraphMock(GraphCandidate candidate) implements GraphModification {

    private static final Class<?>[] TAGS_EMPTY = new Class[]{};

    @Override
    public void accept(ApplicationGraphDraw graphDraw) {
        final Set<Node<Object>> nodesToMock = GraphUtils.findNodeByTypeOrAssignable(graphDraw, candidate());
        if (nodesToMock.isEmpty()) {
            final Class<?>[] tags = (candidate().tags().isEmpty())
                ? TAGS_EMPTY
                : candidate().tagsAsArray();

            graphDraw.addNode0(candidate().type(), tags, getNodeFactory(g -> {
                if (candidate().type() instanceof Class<?> mockClass) {
                    var addition = Mockito.mock(mockClass);
                    if (Lifecycle.class.isAssignableFrom(mockClass)) {
                        Mockito.when(((Lifecycle) addition).init()).thenReturn(Mono.empty());
                        Mockito.when(((Lifecycle) addition).release()).thenReturn(Mono.empty());
                    }

                    return addition;
                } else {
                    throw new IllegalArgumentException("Can't mock type: " + candidate());
                }
            }, graphDraw));
        } else {
            for (Node<Object> nodeToMock : nodesToMock) {
                graphDraw.replaceNode(nodeToMock, g -> {
                    if (candidate().type() instanceof Class<?> mockClass) {
                        var replacement = Mockito.mock(mockClass);
                        if (Lifecycle.class.isAssignableFrom(mockClass)) {
                            Mockito.when(((Lifecycle) replacement).init()).thenReturn(Mono.empty());
                            Mockito.when(((Lifecycle) replacement).release()).thenReturn(Mono.empty());
                        }

                        return replacement;
                    } else {
                        throw new IllegalArgumentException("Can't mock type: " + candidate());
                    }
                });
            }
        }
    }
}
