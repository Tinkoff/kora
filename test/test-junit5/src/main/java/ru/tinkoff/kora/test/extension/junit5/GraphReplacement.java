package ru.tinkoff.kora.test.extension.junit5;

import org.junit.jupiter.api.extension.ExtensionConfigurationException;
import ru.tinkoff.kora.application.graph.ApplicationGraphDraw;
import ru.tinkoff.kora.application.graph.Graph;
import ru.tinkoff.kora.application.graph.Node;

import java.util.Set;
import java.util.function.Function;

record GraphReplacement(Function<KoraAppGraph, ?> function, GraphCandidate candidate) implements GraphModification {

    @Override
    public void accept(ApplicationGraphDraw graphDraw) {
        final Set<Node<Object>> nodesToReplace = GraphUtils.findNodeByTypeOrAssignable(graphDraw, candidate());
        if (nodesToReplace.isEmpty()) {
            throw new ExtensionConfigurationException("Can't find Nodes to Replace: " + candidate());
        }

        for (Node<Object> nodeToReplace : nodesToReplace) {
            graphDraw.replaceNode(nodeToReplace, ((Graph.Factory<Object>) getNodeFactory(function(), graphDraw)));
        }
    }
}
