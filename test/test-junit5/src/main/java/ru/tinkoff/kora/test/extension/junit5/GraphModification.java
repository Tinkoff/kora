package ru.tinkoff.kora.test.extension.junit5;

import ru.tinkoff.kora.application.graph.ApplicationGraphDraw;
import ru.tinkoff.kora.application.graph.Graph;

import javax.annotation.Nonnull;
import java.util.function.Consumer;
import java.util.function.Function;

interface GraphModification extends Consumer<ApplicationGraphDraw> {

    @Nonnull
    GraphCandidate candidate();

    default <V> Graph.Factory<V> getNodeFactory(Function<KoraAppGraph, V> graphFunction, ApplicationGraphDraw graphDraw) {
        return g -> graphFunction.apply(new DefaultKoraAppGraph(graphDraw, g));
    }
}
