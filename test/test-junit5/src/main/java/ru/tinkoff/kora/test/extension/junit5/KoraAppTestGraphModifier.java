package ru.tinkoff.kora.test.extension.junit5;

import org.jetbrains.annotations.NotNull;
import ru.tinkoff.kora.application.graph.ApplicationGraphDraw;

public interface KoraAppTestGraphModifier {

    /**
     * @return Kora Graph Modifier builder used to add or replace nodes inside {@link ApplicationGraphDraw}
     */
    @NotNull
    KoraGraphModifier graph();
}
