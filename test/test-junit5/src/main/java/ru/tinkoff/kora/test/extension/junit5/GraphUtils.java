package ru.tinkoff.kora.test.extension.junit5;

import ru.tinkoff.kora.application.graph.ApplicationGraphDraw;
import ru.tinkoff.kora.application.graph.Node;
import ru.tinkoff.kora.common.Tag;

import java.lang.reflect.Type;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;

final class GraphUtils {

    private static final Class<?>[] TAG_ANY = new Class[]{Tag.Any.class};

    private GraphUtils() {}

    @SuppressWarnings("unchecked")
    static <T> List<Node<T>> findNodeByType(ApplicationGraphDraw graph, Type type, Class<?>[] tags) {
        if (tags == null) {
            final Node<T> node = (Node<T>) graph.findNodeByType(type);
            return (node == null)
                ? List.of()
                : List.of(node);
        } else if (Arrays.equals(TAG_ANY, tags)) {
            final List<Node<T>> nodes = new ArrayList<>();
            for (var graphNode : graph.getNodes()) {
                if (graphNode.type().equals(type)) {
                    nodes.add((Node<T>) graphNode);
                }
            }
            return nodes;
        } else {
            for (var graphNode : graph.getNodes()) {
                if (Arrays.equals(tags, graphNode.tags()) && graphNode.type().equals(type)) {
                    return List.of((Node<T>) graphNode);
                }
            }
        }

        return List.of();
    }
}
