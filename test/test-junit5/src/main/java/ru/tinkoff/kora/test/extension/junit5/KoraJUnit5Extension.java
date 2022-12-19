package ru.tinkoff.kora.test.extension.junit5;

import org.junit.jupiter.api.extension.*;
import org.junit.platform.commons.support.AnnotationSupport;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import ru.tinkoff.kora.annotation.processor.common.AbstractKoraProcessor;
import ru.tinkoff.kora.annotation.processor.common.TestUtils;
import ru.tinkoff.kora.application.graph.ApplicationGraphDraw;
import ru.tinkoff.kora.application.graph.Lifecycle;
import ru.tinkoff.kora.application.graph.RefreshableGraph;
import ru.tinkoff.kora.common.KoraApp;
import ru.tinkoff.kora.kora.app.annotation.processor.KoraAppProcessor;

import javax.annotation.processing.Processor;
import java.lang.reflect.Constructor;
import java.lang.reflect.Executable;
import java.time.Duration;
import java.util.*;
import java.util.concurrent.ConcurrentHashMap;
import java.util.function.Supplier;
import java.util.stream.Stream;

public class KoraJUnit5Extension implements BeforeAllCallback, BeforeEachCallback, ExecutionCondition, BeforeTestExecutionCallback, ParameterResolver, InvocationInterceptor {

    interface Graph {

        void initialize();

        InitializedGraph get();
    }

    static class AbstractGraph implements Graph {

        protected final Logger logger = LoggerFactory.getLogger(getClass());
        protected final Supplier<? extends ApplicationGraphDraw> graphSupplier;
        protected InitializedGraph initializedGraph;

        AbstractGraph(Supplier<? extends ApplicationGraphDraw> graphSupplier) {
            this.graphSupplier = graphSupplier;
        }

        @Override
        public void initialize() {
            final long started = System.nanoTime();
            var graphDraw = graphSupplier.get();
            this.initializedGraph = new InitializedGraph(graphDraw.init().block(), graphDraw);
            logger.info("KoraAppTest Graph Initialization took: {}", Duration.ofNanos(System.nanoTime() - started));
        }

        @Override
        public InitializedGraph get() {
            if (initializedGraph == null) {
                initialize();
            }

            return initializedGraph;
        }
    }

    static class PerMethodGraph extends AbstractGraph {

        PerMethodGraph(Supplier<? extends ApplicationGraphDraw> graphSupplier) {
            super(graphSupplier);
        }
    }

    static class PerClassGraph extends AbstractGraph {

        public PerClassGraph(Supplier<? extends ApplicationGraphDraw> graphSupplier) {
            super(graphSupplier);
        }
    }

    static class PerRunGraph extends AbstractGraph {

        public PerRunGraph(Supplier<? extends ApplicationGraphDraw> graphSupplier) {
            super(graphSupplier);
        }

        @Override
        public void initialize() {
            if (initializedGraph == null) {
                super.initialize();
            }
        }
    }

    record InitializedGraph(RefreshableGraph refreshableGraph, ApplicationGraphDraw graphDraw) {}

    record KoraAppMeta(Class<?> application,
                       List<Class<? extends Lifecycle>> classes,
                       List<Class<? extends AbstractKoraProcessor>> processors,
                       KoraAppTest.CompilationShareMode shareMode) {}

    private static final ExtensionContext.Namespace NAMESPACE = ExtensionContext.Namespace.create(KoraJUnit5Extension.class);

    private static final Map<Class<?>, Processor> PROCESSOR_INSTANCES = new ConcurrentHashMap<>();
    private static final Map<KoraAppMeta, Graph> GRAPH_MAP = new ConcurrentHashMap<>();

    private Graph getGraph(ExtensionContext context) {
        var storage = context.getStore(NAMESPACE);
        var meta = storage.get(KoraAppTest.class, KoraAppMeta.class);
        return GRAPH_MAP.computeIfAbsent(meta, KoraJUnit5Extension::getApplicationGraph);
    }

    @Override
    public void beforeEach(ExtensionContext context) {
        System.out.println("TEST");
    }

    @Override
    public void beforeAll(ExtensionContext context) {
        final KoraAppMeta meta = findKoraAppTest(context)
            .map(koraAppTest -> {
                var classes = Arrays.stream(koraAppTest.classes())
                    .distinct()
                    .sorted(Comparator.comparing(Class::getCanonicalName))
                    .toList();

                var processors = Stream.concat(Arrays.stream(koraAppTest.processors()), Stream.of(KoraAppProcessor.class))
                    .distinct()
                    .sorted(Comparator.comparing(Class::getCanonicalName))
                    .toList();

//                koraAppTest.configuration()
//                koraAppTest.application().getMethods();
                return new KoraAppMeta(koraAppTest.application(), classes, processors, koraAppTest.shareMode());
            })
            .orElseThrow(() -> new ExtensionConfigurationException("@KoraAppTest not found"));

        var storage = context.getStore(NAMESPACE);
        storage.put(KoraAppTest.class, meta);

        var graph = getGraph(context);
        if (graph instanceof PerClassGraph || graph instanceof PerRunGraph) {
            graph.initialize();
        }
    }

    @Override
    public void beforeTestExecution(ExtensionContext context) {
        var graph = getGraph(context);
        if (graph instanceof PerMethodGraph) {
            graph.initialize();
        }
    }

    @Override
    public ConditionEvaluationResult evaluateExecutionCondition(ExtensionContext context) {
        return findKoraAppTest(context)
            .map(koraAppTest -> ConditionEvaluationResult.enabled("KoraAppTest found"))
            .orElseThrow(() -> new ExtensionConfigurationException("@KoraAppTest not found"));
    }

    private Optional<KoraAppTest> findKoraAppTest(ExtensionContext context) {
        Optional<ExtensionContext> current = Optional.of(context);
        while (current.isPresent()) {
            final Optional<KoraAppTest> testcontainers = AnnotationSupport.findAnnotation(current.get().getRequiredTestClass(), KoraAppTest.class);
            if (testcontainers.isPresent()) {
                return testcontainers;
            }

            current = current.get().getParent();
        }

        return Optional.empty();
    }

    @Override
    public boolean supportsParameter(ParameterContext parameterContext, ExtensionContext context) throws ParameterResolutionException {
        var graph = getGraph(context);
        return getComponent(parameterContext, graph.get()) != null;
    }

    @Override
    public Object resolveParameter(ParameterContext parameterContext, ExtensionContext context) throws ParameterResolutionException {
        var graph = getGraph(context);
        return getComponent(parameterContext, graph.get());
    }

    private Object getComponent(ParameterContext parameterContext, InitializedGraph initializedGraph) {
        try {
            final Executable declaringExecutable = parameterContext.getDeclaringExecutable();
            final int index = parameterContext.getIndex();
            final Class<?> parameterType = declaringExecutable.getParameterTypes()[index];
            return getComponentFromGraph(initializedGraph, parameterType);
        } catch (Exception e) {
            return null;
        }
    }

    private static <T> T getComponentFromGraph(InitializedGraph graph, Class<T> targetType) {
        var values = graph.graphDraw().getNodes()
            .stream()
            .map(graph.refreshableGraph()::get)
            .toList();

        return values.stream()
            .filter(a -> targetType.isAssignableFrom(a.getClass()))
            .map(a -> ((T) a))
            .findFirst()
            .orElseThrow(() -> new ExtensionConfigurationException(targetType + " expected type to be " + Lifecycle.class + ", but couldn't find it in generated Graph, please check @KoraAppTest configuration or " + targetType));
    }

    private static Graph getApplicationGraph(KoraAppMeta meta) {
        try {
            final List<Class<?>> classes = new ArrayList<>(meta.classes);
            classes.add(meta.application);

            final List<Processor> processors = meta.processors.stream()
                .map(p -> PROCESSOR_INSTANCES.computeIfAbsent(p, (k) -> instantiateProcessor(p)))
                .toList();

            var classLoader = TestUtils.annotationProcess(classes, processors);
            var clazz = classLoader.loadClass(meta.application.getName() + "Graph");
            var constructors = (Constructor<? extends Supplier<? extends ApplicationGraphDraw>>[]) clazz.getConstructors();
            var graphSupplier = constructors[0].newInstance();

            return switch (meta.shareMode) {
                case PER_RUN -> new PerRunGraph(graphSupplier);
                case PER_CLASS -> new PerClassGraph(graphSupplier);
                case PER_METHOD -> new PerMethodGraph(graphSupplier);
            };
        } catch (ClassNotFoundException e) {
            throw new ExtensionConfigurationException("@KoraAppTest#application must specify class that is annotated with @KoraApp, but class probably wasn't: " + meta.application);
        } catch (Exception e) {
            throw new IllegalStateException(e);
        }
    }

    private static Processor instantiateProcessor(Class<? extends AbstractKoraProcessor> processor) {
        return Arrays.stream(processor.getDeclaredConstructors())
            .filter(c -> c.getParameterCount() == 0)
            .findFirst()
            .map(c -> {
                try {
                    return ((Processor) c.newInstance());
                } catch (Exception e) {
                    throw new ExtensionConfigurationException("KoraAppTest can't instantiate processor: " + processor);
                }
            })
            .orElseThrow(() -> new ExtensionConfigurationException("KoraAppTest can't instantiate processor with NoZeroArgument constructor: " + processor));
    }
}
