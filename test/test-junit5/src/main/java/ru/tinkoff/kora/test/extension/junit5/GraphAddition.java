package ru.tinkoff.kora.test.extension.junit5;

import ru.tinkoff.kora.application.graph.ApplicationGraphDraw;

import java.util.function.Function;

record GraphAddition(Function<KoraAppGraph, ?> function, GraphCandidate candidate) implements GraphModification {

    private static final Class<?>[] TAGS_EMPTY = new Class[]{};

    @Override
    public void accept(ApplicationGraphDraw graphDraw) {
        final Class<?>[] tags = (candidate().tags() == null)
            ? TAGS_EMPTY
            : candidate().tags();

        graphDraw.addNode0(candidate().type(), tags, getNodeFactory(function(), graphDraw));
    }
}
