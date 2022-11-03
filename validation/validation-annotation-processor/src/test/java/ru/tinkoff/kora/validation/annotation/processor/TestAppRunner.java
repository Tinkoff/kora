package ru.tinkoff.kora.validation.annotation.processor;

import org.junit.jupiter.api.Assertions;
import ru.tinkoff.kora.annotation.processor.common.TestUtils;
import ru.tinkoff.kora.application.graph.ApplicationGraphDraw;
import ru.tinkoff.kora.application.graph.RefreshableGraph;
import ru.tinkoff.kora.config.annotation.processor.processor.ConfigRootAnnotationProcessor;
import ru.tinkoff.kora.config.annotation.processor.processor.ConfigSourceAnnotationProcessor;
import ru.tinkoff.kora.kora.app.annotation.processor.KoraAppProcessor;
import ru.tinkoff.kora.validation.annotation.processor.testdata.AppWithConfig;
import ru.tinkoff.kora.validation.annotation.processor.testdata.Bar;
import ru.tinkoff.kora.validation.annotation.processor.testdata.Foo;

import java.lang.reflect.Constructor;
import java.util.ArrayList;
import java.util.List;
import java.util.function.Supplier;

public abstract class TestAppRunner extends Assertions {

    public record InitializedGraph(RefreshableGraph refreshableGraph, ApplicationGraphDraw graphDraw) {}

    private InitializedGraph graph = null;

    protected InitializedGraph getGraph() {
        if (graph == null) {
            graph = getGraph(AppWithConfig.class, Foo.class, Bar.class);
        }
        return graph;
    }

    protected InitializedGraph getGraph(Class<?> app, Class<?>... targetClasses) {
        try {
            final List<Class<?>> classes = new ArrayList<>(List.of(targetClasses));
            classes.add(app);
            var classLoader = TestUtils.annotationProcess(classes, new KoraAppProcessor(), new ConfigRootAnnotationProcessor(), new ConfigSourceAnnotationProcessor(), new ValidationAnnotationProcessor());
            var clazz = classLoader.loadClass(app.getName() + "Graph");
            var constructors = (Constructor<? extends Supplier<? extends ApplicationGraphDraw>>[]) clazz.getConstructors();
            var graphDraw = constructors[0].newInstance().get();
            return new InitializedGraph(graphDraw.init().block(), graphDraw);
        } catch (Exception e) {
            if (e.getCause() != null) {
                throw new IllegalStateException(e.getCause());
            }

            throw new IllegalStateException(e);
        }
    }

    protected <T> T getService(Class<T> tClass) {
        final InitializedGraph graph = getGraph();
        final List<?> nodeValues = graph.graphDraw().getNodes().stream()
            .map(n -> graph.refreshableGraph().get(n))
            .toList();

        return nodeValues.stream()
            .filter(v -> v.getClass().isAssignableFrom(tClass))
            .map(v -> ((T) v))
            .findFirst().orElseThrow();
    }
}
