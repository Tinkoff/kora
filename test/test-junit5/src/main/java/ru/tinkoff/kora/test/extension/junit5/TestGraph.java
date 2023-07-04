package ru.tinkoff.kora.test.extension.junit5;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import ru.tinkoff.kora.application.graph.ApplicationGraphDraw;
import ru.tinkoff.kora.application.graph.RefreshableGraph;
import ru.tinkoff.kora.test.extension.junit5.KoraJUnit5Extension.TestClassMetadata;

import javax.annotation.Nonnull;
import javax.annotation.Nullable;
import java.io.IOException;
import java.time.Duration;
import java.util.function.Supplier;

final class TestGraph implements AutoCloseable {

    private static final Object LOCK = new Object();

    private static final Logger logger = LoggerFactory.getLogger(KoraJUnit5Extension.class);

    @Nullable
    private final KoraGraphModification graphModifier;
    private final Supplier<? extends ApplicationGraphDraw> graphSupplier;
    private final TestClassMetadata meta;

    private volatile TestGraphInitialized graphInitialized;

    TestGraph(Supplier<? extends ApplicationGraphDraw> graphSupplier,
              TestClassMetadata meta,
              @Nullable KoraGraphModification graphModifier) {
        this.graphSupplier = graphSupplier;
        this.graphModifier = graphModifier;
        this.meta = meta;
    }

    void initialize() {
        logger.debug("@KoraAppTest initializing graph...");
        final long started = System.nanoTime();

        synchronized (LOCK) {
            var config = meta.config();
            try {
                var graphDraw = graphSupplier.get();
                if (graphModifier != null) {
                    for (GraphModification modification : graphModifier.getModifications()) {
                        modification.accept(graphDraw);
                    }
                }

                config.setup(graphDraw);

                final RefreshableGraph initGraph = graphDraw.init().block(Duration.ofMinutes(10));
                this.graphInitialized = new TestGraphInitialized(initGraph, graphDraw, new DefaultKoraAppGraph(graphDraw, initGraph));
                logger.info("@KoraAppTest initialization took: {}", Duration.ofNanos(System.nanoTime() - started));
            } catch (IOException e) {
                throw new IllegalStateException(e);
            } finally {
                config.cleanup();
            }
        }
    }

    @Nonnull
    TestGraphInitialized initialized() {
        if (graphInitialized == null) {
            throw new IllegalStateException("TestGraphInitialized is not initialized!");
        }
        return graphInitialized;
    }

    @Override
    public void close() {
        if (graphInitialized != null) {
            final long started = System.nanoTime();
            logger.debug("@KoraAppTest releasing graph...");
            graphInitialized.refreshableGraph().release().block(Duration.ofMinutes(10));
            graphInitialized = null;
            logger.info("@KoraAppTest releasing took: {}", Duration.ofNanos(System.nanoTime() - started));
        }
    }
}
