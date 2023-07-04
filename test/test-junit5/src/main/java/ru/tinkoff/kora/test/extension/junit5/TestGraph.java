package ru.tinkoff.kora.test.extension.junit5;

import org.junit.jupiter.api.extension.ExtensionContext;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import ru.tinkoff.kora.application.graph.*;
import ru.tinkoff.kora.test.extension.junit5.KoraAppTest.InitializeMode;

import javax.annotation.Nonnull;
import javax.annotation.Nullable;
import java.time.Duration;
import java.util.function.Supplier;

final class TestGraph implements ExtensionContext.Store.CloseableResource, AutoCloseable {

    private static final Logger logger = LoggerFactory.getLogger(KoraJUnit5Extension.class);

    @Nullable
    private final KoraGraphModification graphModifier;
    private final Supplier<? extends ApplicationGraphDraw> graphSupplier;
    private final InitializeMode initializeMode;
    private volatile TestGraphInitialized graphInitialized;

    TestGraph(Supplier<? extends ApplicationGraphDraw> graphSupplier,
              @Nullable KoraGraphModification graphModifier,
              InitializeMode initializeMode) {
        this.graphSupplier = graphSupplier;
        this.graphModifier = graphModifier;
        this.initializeMode = initializeMode;
    }

    void initialize() {
        var graphDraw = graphSupplier.get();
        if (graphModifier != null) {
            final long startedModify = System.nanoTime();

            for (GraphModification modification : graphModifier.getModifications()) {
                modification.accept(graphDraw);
            }

            logger.debug("@KoraAppTest modification took: {}", Duration.ofNanos(System.nanoTime() - startedModify));
        }

        final long startedInit = System.nanoTime();
        final RefreshableGraph initGraph = graphDraw.init().block(Duration.ofMinutes(10));
        this.graphInitialized = new TestGraphInitialized(initGraph, graphDraw, new DefaultKoraAppGraph(graphDraw, initGraph));
        logger.info("@KoraAppTest initialization took: {}", Duration.ofNanos(System.nanoTime() - startedInit));
    }

    @Nonnull
    InitializeMode initializeMode() {
        return initializeMode;
    }

    TestGraphInitialized initialized() {
        if (graphInitialized == null) {
            initialize();
        }

        return graphInitialized;
    }

    @Override
    public void close() {
        if(graphInitialized != null) {
            graphInitialized.refreshableGraph().release().block(Duration.ofMinutes(10));
            graphInitialized = null;
        }
    }
}
