package ru.tinkoff.kora.test.extension.junit5;

import ru.tinkoff.kora.application.graph.ApplicationGraphDraw;
import ru.tinkoff.kora.application.graph.Node;
import ru.tinkoff.kora.common.Tag;

import java.lang.reflect.ParameterizedType;
import java.lang.reflect.Type;
import java.util.*;

final class GraphUtils {

    private static final Class<?>[] TAG_ANY = new Class[]{Tag.Any.class};

    private GraphUtils() {}

    @SuppressWarnings("unchecked")
    static <T> Set<Node<T>> findNodeByType(ApplicationGraphDraw graph, Type type, Class<?>[] tags) {
        if (tags == null) {
            final Node<T> node = (Node<T>) graph.findNodeByType(type);
            return (node == null)
                ? Set.of()
                : Set.of(node);
        } else if (Arrays.equals(TAG_ANY, tags)) {
            final Set<Node<T>> nodes = new HashSet<>();
            for (var graphNode : graph.getNodes()) {
                if (graphNode.type().equals(type)) {
                    nodes.add((Node<T>) graphNode);
                }
            }
            return nodes;
        } else {
            for (var graphNode : graph.getNodes()) {
                if (Arrays.equals(tags, graphNode.tags()) && graphNode.type().equals(type)) {
                    return Set.of((Node<T>) graphNode);
                }
            }
        }

        return Set.of();
    }

    @SuppressWarnings("unchecked")
    static <T> Set<Node<T>> findNodeByTypeOrAssignable(ApplicationGraphDraw graph, Type type, Class<?>[] tags) {
        if (tags == null) {
            final Set<Node<T>> nodes = new HashSet<>();
            for (var graphNode : graph.getNodes()) {
                if (graphNode.type().equals(type)) {
                    nodes.add((Node<T>) graphNode);
                }

                var typeClass = tryCastType(type);
                var graphClass = tryCastType(graphNode.type());
                if (typeClass.isPresent() && graphClass.isPresent() && typeClass.get().isAssignableFrom(graphClass.get())) {
                    nodes.add((Node<T>) graphNode);
                }
            }

            return nodes;
        } else if (Arrays.equals(TAG_ANY, tags)) {
            final Set<Node<T>> nodes = new HashSet<>();
            for (var graphNode : graph.getNodes()) {
                if (graphNode.type().equals(type)) {
                    nodes.add((Node<T>) graphNode);
                }

                var typeClass = tryCastType(type);
                var graphClass = tryCastType(graphNode.type());
                if (typeClass.isPresent() && graphClass.isPresent() && typeClass.get().isAssignableFrom(graphClass.get())) {
                    nodes.add((Node<T>) graphNode);
                }
            }

            return nodes;
        } else {
            for (var graphNode : graph.getNodes()) {
                if (Arrays.equals(tags, graphNode.tags())) {
                    if (graphNode.type().equals(type)) {
                        return Set.of((Node<T>) graphNode);
                    }

                    var typeClass = tryCastType(type);
                    var graphClass = tryCastType(graphNode.type());
                    if (typeClass.isPresent() && graphClass.isPresent() && typeClass.get().isAssignableFrom(graphClass.get())) {
                        return Set.of((Node<T>) graphNode);
                    }
                }
            }
        }

        return Set.of();
    }

    static Optional<Class<?>> tryCastType(Type type) {
        try {
            if (type instanceof Class<?> tc) {
                return Optional.of(tc);
            } else if (type instanceof ParameterizedType tp) {
                return (tp.getRawType() instanceof Class<?>)
                    ? Optional.ofNullable(((Class<?>) tp.getRawType()))
                    : Optional.ofNullable(KoraJUnit5Extension.class.getClassLoader().loadClass(tp.getRawType().getTypeName()));
            } else {
                return Optional.empty();
            }
        } catch (ClassNotFoundException e) {
            return Optional.empty();
        }
    }
}
