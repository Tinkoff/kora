package ru.tinkoff.kora.test.extension.junit5;

import com.typesafe.config.Config;
import com.typesafe.config.ConfigFactory;
import org.jetbrains.annotations.NotNull;
import org.junit.jupiter.api.extension.*;
import org.junit.platform.commons.support.AnnotationSupport;
import org.mockito.Mockito;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import reactor.core.publisher.Mono;
import ru.tinkoff.kora.application.graph.ApplicationGraphDraw;
import ru.tinkoff.kora.application.graph.Graph.Factory;
import ru.tinkoff.kora.application.graph.Lifecycle;
import ru.tinkoff.kora.application.graph.Node;
import ru.tinkoff.kora.application.graph.RefreshableGraph;
import ru.tinkoff.kora.common.Component;
import ru.tinkoff.kora.common.Tag;
import ru.tinkoff.kora.config.common.ConfigModule;
import ru.tinkoff.kora.test.extension.junit5.KoraGraphModification.NodeMock;
import ru.tinkoff.kora.test.extension.junit5.KoraGraphModification.NodeTypeCandidate;

import javax.annotation.Nullable;
import java.lang.annotation.Annotation;
import java.lang.reflect.Constructor;
import java.lang.reflect.Field;
import java.lang.reflect.Modifier;
import java.lang.reflect.Type;
import java.time.Duration;
import java.util.*;
import java.util.concurrent.ConcurrentHashMap;
import java.util.function.Function;
import java.util.function.Supplier;
import java.util.stream.Collectors;
import java.util.stream.Stream;

final class KoraJUnit5Extension implements BeforeAllCallback, BeforeEachCallback, ExecutionCondition, ParameterResolver, InvocationInterceptor {

    private static final Logger logger = LoggerFactory.getLogger(KoraJUnit5Extension.class);
    private static final ExtensionContext.Namespace NAMESPACE = ExtensionContext.Namespace.create(KoraJUnit5Extension.class);
    private static final Class<?>[] TAG_ANY = new Class[]{Tag.Any.class};

    private static final Map<KoraAppMeta, GraphSupplier> GRAPH_SUPPLIER_MAP = new ConcurrentHashMap<>();

    static class TestClassContainer {

        final KoraAppTest koraAppTest;
        volatile Graph graph;

        TestClassContainer(KoraAppTest koraAppTest) {
            this.koraAppTest = koraAppTest;
        }
    }

    record GraphSupplier(Supplier<? extends ApplicationGraphDraw> graphSupplier, KoraAppMeta meta) {

        public Graph get() {
            return switch (meta.shareMode) {
                case PER_RUN -> new PerRunGraph(graphSupplier, meta.components, meta.graphModifier);
                case PER_CLASS -> new PerClassGraph(graphSupplier, meta.components, meta.graphModifier);
                case PER_METHOD -> new PerMethodGraph(graphSupplier, meta.components, meta.graphModifier);
            };
        }
    }

    interface Graph {

        void initialize();

        GraphInitialized initialized();
    }

    @SuppressWarnings("unchecked")
    static class AbstractGraph implements Graph {

        private static final Class<?>[] TAGS_EMPTY = new Class[]{};

        @Nullable
        protected final KoraGraphModification graphModifier;
        protected final Collection<NodeTypeCandidate> components;
        protected final Supplier<? extends ApplicationGraphDraw> graphSupplier;
        protected volatile GraphInitialized graphInitialized;

        AbstractGraph(Supplier<? extends ApplicationGraphDraw> graphSupplier,
                      Collection<NodeTypeCandidate> components,
                      @Nullable KoraGraphModification graphModifier) {
            this.graphSupplier = graphSupplier;
            this.components = components;
            this.graphModifier = graphModifier;
        }

        @Override
        public void initialize() {
            var graphDraw = graphSupplier.get();
            if (graphModifier != null) {
                final long startedModify = System.nanoTime();

                for (KoraGraphModification.NodeAddition addition : graphModifier.getAdditions()) {
                    final Class<?>[] tags = (addition.candidate().tags() == null)
                        ? TAGS_EMPTY
                        : addition.candidate().tags();

                    graphDraw.addNode0(addition.candidate().type(), tags, getNodeFactory(addition.function(), graphDraw));
                }

                for (KoraGraphModification.NodeReplacement replacement : graphModifier.getReplacements()) {
                    final Set<Node<Object>> nodesToReplace = GraphUtils.findNodeByType(graphDraw, replacement.candidate().type(), replacement.candidate().tags());
                    if (nodesToReplace.isEmpty()) {
                        throw new ExtensionConfigurationException("Can't find Nodes to Replace: " + replacement.candidate());
                    }

                    for (Node<Object> nodeToReplace : nodesToReplace) {
                        graphDraw.replaceNode(nodeToReplace, ((Factory<Object>) getNodeFactory(replacement.function(), graphDraw)));
                    }
                }

                for (NodeMock mock : graphModifier.getMocks()) {
                    final Set<Node<Object>> nodesToMock = GraphUtils.findNodeByTypeOrAssignable(graphDraw, mock.candidate().type(), mock.candidate().tags());
                    if (nodesToMock.isEmpty()) {
                        final Class<?>[] tags = (mock.candidate().tags() == null)
                            ? TAGS_EMPTY
                            : mock.candidate().tags();

                        graphDraw.addNode0(mock.candidate().type(), tags, getNodeFactory(g -> {
                            if (mock.candidate().type() instanceof Class<?> mockClass) {
                                var addition = Mockito.mock(mockClass);
                                if (Lifecycle.class.isAssignableFrom(mockClass)) {
                                    Mockito.when(((Lifecycle) addition).init()).thenReturn(Mono.empty());
                                    Mockito.when(((Lifecycle) addition).release()).thenReturn(Mono.empty());
                                }

                                return addition;
                            } else {
                                throw new IllegalArgumentException("Can't mock type: " + mock.candidate().type());
                            }
                        }, graphDraw));
                    } else {
                        for (Node<Object> nodeToMock : nodesToMock) {
                            graphDraw.replaceNode(nodeToMock, g -> {
                                if (mock.candidate().type() instanceof Class<?> mockClass) {
                                    var replacement = Mockito.mock(mockClass);
                                    if (Lifecycle.class.isAssignableFrom(mockClass)) {
                                        Mockito.when(((Lifecycle) replacement).init()).thenReturn(Mono.empty());
                                        Mockito.when(((Lifecycle) replacement).release()).thenReturn(Mono.empty());
                                    }

                                    return replacement;
                                } else {
                                    throw new IllegalArgumentException("Can't mock type: " + mock.candidate().type());
                                }
                            });
                        }
                    }
                }

                logger.debug("@KoraAppTest modification took: {}", Duration.ofNanos(System.nanoTime() - startedModify));
            }

            final long startedInit = System.nanoTime();
            final RefreshableGraph initGraph = graphDraw.init().block(Duration.ofMinutes(3));
            this.graphInitialized = new GraphInitialized(initGraph, graphDraw);
            logger.info("@KoraAppTest initialization took: {}", Duration.ofNanos(System.nanoTime() - startedInit));
        }

        private <V> Factory<V> getNodeFactory(Function<KoraAppGraph, V> graphFunction, ApplicationGraphDraw graphDraw) {
            return g -> graphFunction.apply(new KoraAppGraph() {

                @Nullable
                @Override
                public Object getFirst(@NotNull Type type) {
                    var node = graphDraw.findNodeByType(type);
                    return (node == null)
                        ? null
                        : g.get(node);
                }

                @Nullable
                @Override
                public <T> T getFirst(@NotNull Class<T> type) {
                    return (T) getFirst(((Type) type));
                }

                @Nullable
                @Override
                public Object getFirst(@NotNull Type type, Class<?>... tags) {
                    var nodes = GraphUtils.findNodeByType(graphDraw, type, tags);
                    return nodes.stream()
                        .map(g::get)
                        .findFirst()
                        .orElse(null);
                }

                @Nullable
                @Override
                public <T> T getFirst(@NotNull Class<T> type, Class<?>... tags) {
                    return (T) getFirst((Type) type, tags);
                }

                @NotNull
                @Override
                public List<Object> getAll(@NotNull Type type) {
                    return getAll(type, TAG_ANY);
                }

                @NotNull
                @Override
                public List<Object> getAll(@NotNull Type type, Class<?>... tags) {
                    var nodes = GraphUtils.findNodeByType(graphDraw, type, tags);
                    return nodes.stream()
                        .map(g::get)
                        .toList();
                }

                @NotNull
                @Override
                public <T> List<T> getAll(@NotNull Class<T> type) {
                    return getAll(type, TAG_ANY);
                }

                @NotNull
                @Override
                public <T> List<T> getAll(@NotNull Class<T> type, Class<?>... tags) {
                    return (List<T>) getAll(((Type) type), tags);
                }
            });
        }

        @Override
        public GraphInitialized initialized() {
            if (graphInitialized == null) {
                initialize();
            }

            return graphInitialized;
        }
    }

    static class PerMethodGraph extends AbstractGraph {

        PerMethodGraph(Supplier<? extends ApplicationGraphDraw> graphSupplier, Collection<NodeTypeCandidate> components, KoraGraphModification graphModifier) {
            super(graphSupplier, components, graphModifier);
        }
    }

    static class PerClassGraph extends AbstractGraph {

        public PerClassGraph(Supplier<? extends ApplicationGraphDraw> graphSupplier, Collection<NodeTypeCandidate> components, KoraGraphModification graphModifier) {
            super(graphSupplier, components, graphModifier);
        }
    }

    static class PerRunGraph extends AbstractGraph {

        public PerRunGraph(Supplier<? extends ApplicationGraphDraw> graphSupplier, Collection<NodeTypeCandidate> components, KoraGraphModification graphModifier) {
            super(graphSupplier, components, graphModifier);
        }

        @Override
        public void initialize() {
            if (graphInitialized == null) {
                super.initialize();
            }
        }
    }

    record GraphInitialized(RefreshableGraph refreshableGraph, ApplicationGraphDraw graphDraw) {}

    record KoraAppMeta(Class<?> application,
                       String configuration,
                       Set<NodeTypeCandidate> components,
                       KoraAppTest.InitializeMode shareMode,
                       @Nullable KoraGraphModification graphModifier) {}

    record TestComponentCandidate(Type type, Class<?>[] tags) {

        @Override
        public String toString() {
            return "[type=" + type + ", tags=" + Arrays.toString(tags) + ']';
        }
    }

    private static TestClassContainer getContainer(ExtensionContext context) {
        var storage = context.getStore(NAMESPACE);
        return storage.get(KoraAppTest.class, TestClassContainer.class);
    }

    @Override
    public void beforeEach(ExtensionContext context) {
        var container = getContainer(context);
        if (container.graph == null) {
            final KoraAppMeta meta = findKoraAppTest(context)
                .map(koraAppTest -> getKoraAppMeta(koraAppTest, context))
                .orElseThrow(() -> new ExtensionConfigurationException("@KoraAppTest not found"));

            var graphSupplier = GRAPH_SUPPLIER_MAP.computeIfAbsent(meta, KoraJUnit5Extension::generateGraphSupplier);
            var graph = graphSupplier.get();
            graph.initialize();
            container.graph = graph;
        } else if (container.graph instanceof PerMethodGraph) {
            container.graph.initialize();
        }

        var testInstance = context.getTestInstance().orElseThrow(() -> new ExtensionConfigurationException("@KoraAppTest can't get TestInstance for @TestComponent field injection"));
        injectTestComponentFields(testInstance, container.graph.initialized());
        injectMockComponentFields(testInstance, container.graph.initialized());
    }

    private static void injectTestComponentFields(Object o, GraphInitialized graphInitialized) {
        injectByAnnotationFields(o, graphInitialized, TestComponent.class);
    }

    private static void injectMockComponentFields(Object o, GraphInitialized graphInitialized) {
        injectByAnnotationFields(o, graphInitialized, MockComponent.class);
    }

    private static void injectByAnnotationFields(Object o, GraphInitialized graphInitialized, Class<? extends Annotation> annotation) {
        final List<Field> fieldsForInjection = Arrays.stream(o.getClass().getDeclaredFields())
            .filter(f -> !f.isSynthetic())
            .filter(f -> Arrays.stream(f.getDeclaredAnnotations()).anyMatch(a -> a.annotationType().equals(annotation)))
            .toList();

        if (fieldsForInjection.isEmpty()) {
            return;
        }

        var finalFields = fieldsForInjection.stream()
            .filter(f -> Modifier.isFinal(f.getModifiers()))
            .toList();

        if (!finalFields.isEmpty()) {
            throw new ExtensionConfigurationException(finalFields.stream()
                .map(f -> f.getDeclaringClass().getCanonicalName() + "#" + f.getName())
                .collect(Collectors.joining(", ", "Fields annotated @" + annotation.getSimpleName() + " have illegal 'final' modifier [", "]")));
        }

        var staticFields = fieldsForInjection.stream()
            .filter(f -> Modifier.isStatic(f.getModifiers()))
            .toList();

        if (!staticFields.isEmpty()) {
            throw new ExtensionConfigurationException(staticFields.stream()
                .map(f -> f.getDeclaringClass().getCanonicalName() + "#" + f.getName())
                .collect(Collectors.joining(", ", "Fields annotated @" + annotation.getSimpleName() + " have illegal 'static' modifier [", "]")));
        }

        for (Field field : fieldsForInjection) {
            final Class<?>[] tags = Arrays.stream(field.getDeclaredAnnotations())
                .filter(a -> a.annotationType().equals(Tag.class))
                .map(a -> ((Tag) a).value())
                .findFirst()
                .orElse(null);

            final TestComponentCandidate candidate = new TestComponentCandidate(field.getType(), tags);
            final Object component = getComponentOrThrow(graphInitialized, candidate);
            try {
                field.setAccessible(true);
                field.set(o, component);
            } catch (Exception e) {
                throw new ExtensionConfigurationException("Failed to Inject field '" + field.getName() + "' due to: " + e);
            }
        }
    }

    @Override
    public void beforeAll(ExtensionContext context) {
        final KoraAppTest appTest = findKoraAppTest(context)
            .orElseThrow(() -> new ExtensionConfigurationException("@KoraAppTest not found"));

        var storage = context.getStore(NAMESPACE);
        storage.put(KoraAppTest.class, new TestClassContainer(appTest));
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
            final Optional<KoraAppTest> annotation = AnnotationSupport.findAnnotation(current.get().getRequiredTestClass(), KoraAppTest.class);
            if (annotation.isPresent()) {
                return annotation;
            }

            current = current.get().getParent();
        }

        return Optional.empty();
    }

    @Override
    public boolean supportsParameter(ParameterContext parameterContext, ExtensionContext context) throws ParameterResolutionException {
        return Arrays.stream(parameterContext.getParameter().getDeclaredAnnotations())
            .anyMatch(a -> a.annotationType().equals(TestComponent.class) || a.annotationType().equals(MockComponent.class));
    }

    @Override
    public Object resolveParameter(ParameterContext parameterContext, ExtensionContext context) throws ParameterResolutionException {
        var container = getContainer(context);
        var candidate = getTestComponentCandidate(parameterContext);
        return getComponentOrThrow(container.graph.initialized(), candidate);
    }

    private KoraAppMeta getKoraAppMeta(KoraAppTest koraAppTest, ExtensionContext context) {
        final long started = System.nanoTime();

        var testInstance = context.getTestInstance().orElseThrow(() -> new ExtensionConfigurationException("@KoraAppTest can't get TestInstance for @TestComponent field injection"));
        var mockComponentFromFields = Arrays.stream(testInstance.getClass().getDeclaredFields())
            .filter(f -> !f.isSynthetic())
            .filter(f -> Arrays.stream(f.getDeclaredAnnotations()).anyMatch(a -> a.annotationType().equals(MockComponent.class)))
            .map(field -> {
                final Class<?>[] tag = Optional.ofNullable(field.getAnnotation(Tag.class))
                    .map(Tag::value)
                    .orElse(null);

                return new NodeMock(new NodeTypeCandidate(field.getGenericType(), tag));
            })
            .toList();

        var mockComponentFromParameters = context.getTestMethod()
            .filter(method -> !method.isSynthetic())
            .map(method -> List.of(method.getParameters()))
            .map(parameters -> parameters.stream()
                .filter(parameter -> parameter.getDeclaredAnnotation(MockComponent.class) != null)
                .map(parameter -> {
                    final Class<?>[] tag = Optional.ofNullable(parameter.getDeclaredAnnotation(Tag.class))
                        .map(Tag::value)
                        .orElse(null);

                    return new NodeMock(new NodeTypeCandidate(parameter.getParameterizedType(), tag));
                })
                .toList())
            .orElse(List.of());

        var mockComponentFromAnnotation = Arrays.stream(koraAppTest.mocks())
            .map(mock -> new NodeMock(new NodeTypeCandidate(mock)))
            .distinct()
            .sorted(Comparator.comparing(n -> n.candidate().type().getTypeName()))
            .toList();

        final KoraGraphModification koraGraphModification = context.getTestInstance()
            .filter(inst -> inst instanceof KoraAppTestGraph)
            .map(inst -> ((KoraAppTestGraph) inst).graph())
            .map(graph -> {
                mockComponentFromAnnotation.forEach(m -> graph.mockComponent(m.candidate().type(), m.candidate().tags()));
                mockComponentFromFields.forEach(m -> graph.mockComponent(m.candidate().type(), m.candidate().tags()));
                mockComponentFromParameters.forEach(m -> graph.mockComponent(m.candidate().type(), m.candidate().tags()));
                return graph;
            })
            .orElseGet(() -> {
                if (mockComponentFromAnnotation.isEmpty() && mockComponentFromFields.isEmpty() && mockComponentFromParameters.isEmpty()) {
                    return null;
                } else {
                    var graph = KoraGraphModification.of();
                    mockComponentFromAnnotation.forEach(m -> graph.mockComponent(m.candidate().type(), m.candidate().tags()));
                    mockComponentFromFields.forEach(m -> graph.mockComponent(m.candidate().type(), m.candidate().tags()));
                    mockComponentFromParameters.forEach(m -> graph.mockComponent(m.candidate().type(), m.candidate().tags()));
                    return graph;
                }
            });

        Stream<Class<?>> modifierClasses;
        if (koraGraphModification == null) {
            modifierClasses = Stream.empty();
        } else {
            modifierClasses = Stream.concat(
                    koraGraphModification.getMocks().stream().map(m -> m.candidate().type()),
                    koraGraphModification.getReplacements().stream().map(r -> r.candidate().type()))
                .filter(t -> t instanceof Class<?>)
                .map(t -> ((Class<?>) t));
        }

        final List<NodeTypeCandidate> testComponentsFromAnnotation = Stream.concat(Arrays.stream(koraAppTest.components()), modifierClasses)
            .map(NodeTypeCandidate::new)
            .toList();

        final List<NodeTypeCandidate> testComponentFromFields = Arrays.stream(testInstance.getClass().getDeclaredFields())
            .filter(f -> !f.isSynthetic())
            .filter(f -> Arrays.stream(f.getDeclaredAnnotations()).anyMatch(a -> a.annotationType().equals(TestComponent.class)))
            .map(f -> {
                final Class<?>[] tag = Optional.ofNullable(f.getDeclaredAnnotation(Tag.class))
                    .map(Tag::value)
                    .orElse(TAG_ANY);

                return new NodeTypeCandidate(f.getGenericType(), tag);
            })
            .toList();

        final List<NodeTypeCandidate> testComponentFromParameters = context.getTestMethod()
            .filter(method -> !method.isSynthetic())
            .map(method -> List.of(method.getParameters()))
            .map(parameters -> parameters.stream()
                .filter(parameter -> parameter.getDeclaredAnnotation(TestComponent.class) != null)
                .map(parameter -> {
                    final Class<?>[] tag = Optional.ofNullable(parameter.getDeclaredAnnotation(Tag.class))
                        .map(Tag::value)
                        .orElse(TAG_ANY);

                    return new NodeTypeCandidate(parameter.getParameterizedType(), tag);
                })
                .toList())
            .orElse(List.of());

        final Set<NodeTypeCandidate> testComponentsResult = new HashSet<>();
        testComponentsResult.addAll(testComponentsFromAnnotation);
        testComponentsResult.addAll(testComponentFromFields);
        testComponentsResult.addAll(testComponentFromParameters);

        final String koraAppConfig = context.getTestInstance()
            .filter(inst -> inst instanceof KoraAppTestConfig)
            .map(inst -> {
                final KoraConfigModification configModification = ((KoraAppTestConfig) inst).config();
                return configModification.getConfig();
            })
            .orElse("");

        if (koraAppConfig.isBlank()) {
            logger.debug("@KoraAppTest preparation took: {}", Duration.ofNanos(System.nanoTime() - started));
            return new KoraAppMeta(koraAppTest.application(), koraAppConfig,
                testComponentsResult, koraAppTest.initializeMode(), koraGraphModification);
        }

        final KoraGraphModification graphModificationWithConfig = (koraGraphModification == null)
            ? KoraGraphModification.of()
            : koraGraphModification;

        if (ConfigModule.class.isAssignableFrom(koraAppTest.application())) {
            graphModificationWithConfig.replaceComponent(Config.class, () -> ConfigFactory.parseString(koraAppConfig));
        } else {
            graphModificationWithConfig.addComponent(Config.class, () -> ConfigFactory.parseString(koraAppConfig));
        }

        logger.debug("@KoraAppTest preparation took: {}", Duration.ofNanos(System.nanoTime() - started));
        return new KoraAppMeta(koraAppTest.application(), koraAppConfig,
            testComponentsResult, koraAppTest.initializeMode(), graphModificationWithConfig);
    }

    private TestComponentCandidate getTestComponentCandidate(ParameterContext parameterContext) {
        final Type parameterType = parameterContext.getParameter().getParameterizedType();
        final Class<?>[] tags = Arrays.stream(parameterContext.getParameter().getDeclaredAnnotations())
            .filter(a -> a.annotationType().equals(Tag.class))
            .map(a -> ((Tag) a).value())
            .findFirst()
            .orElse(null);

        return new TestComponentCandidate(parameterType, tags);
    }

    private static Object getComponentOrThrow(GraphInitialized graphInitialized, TestComponentCandidate candidate) {
        return getComponent(graphInitialized, candidate)
            .orElseThrow(() -> new ExtensionConfigurationException(candidate + " expected type to implement " + Lifecycle.class + " or be a " + Component.class
                                                                   + ", but it was not present generated graph, please check @KoraAppTest configuration for " + candidate));
    }

    private static Optional<Object> getComponent(GraphInitialized graphInitialized, TestComponentCandidate candidate) {
        try {
            return getComponentFromGraph(graphInitialized, candidate);
        } catch (Exception e) {
            logger.warn(e.getMessage());
            return Optional.empty();
        }
    }

    private static Optional<Object> getComponentFromGraph(GraphInitialized graph, TestComponentCandidate candidate) {
        if (candidate.tags() == null) {
            return Optional.ofNullable(graph.graphDraw().findNodeByType(candidate.type()))
                .map(v -> ((Object) graph.refreshableGraph().get(v)))
                .or(() -> {
                    // Try to find similar
                    return graph.graphDraw().getNodes()
                        .stream()
                        .map(graph.refreshableGraph()::get)
                        .filter(v -> {
                            if (candidate.type() instanceof Class<?> tc) {
                                return tc.isAssignableFrom(v.getClass());
                            } else {
                                return false;
                            }
                        })
                        .findFirst();
                });
        } else {
            return Optional.of(GraphUtils.findNodeByType(graph.graphDraw(), candidate.type(), candidate.tags()))
                .filter(l -> !l.isEmpty())
                .map(v -> graph.refreshableGraph().get(v.iterator().next()))
                .or(() -> {
                    // Try to find similar
                    return graph.graphDraw().getNodes()
                        .stream()
                        .filter(n -> Arrays.equals(n.tags(), candidate.tags()))
                        .map(graph.refreshableGraph()::get)
                        .filter(v -> {
                            if (candidate.type() instanceof Class<?> tc) {
                                return tc.isAssignableFrom(v.getClass());
                            } else {
                                return false;
                            }
                        })
                        .findFirst();
                });
        }
    }

    @SuppressWarnings("unchecked")
    private static GraphSupplier generateGraphSupplier(KoraAppMeta meta) {
        try {
            final long startedLoading = System.nanoTime();
            var clazz = KoraJUnit5Extension.class.getClassLoader().loadClass(meta.application.getName() + "Graph");
            var constructors = (Constructor<? extends Supplier<? extends ApplicationGraphDraw>>[]) clazz.getConstructors();
            var graphSupplier = constructors[0].newInstance();
            logger.debug("@KoraAppTest loading took: {}", Duration.ofNanos(System.nanoTime() - startedLoading));

            final long startedSubgraph = System.nanoTime();
            final ApplicationGraphDraw graphDraw = graphSupplier.get();
            final Node<?>[] nodesForSubGraph = meta.components.stream()
                .map(component -> {
                    for (var graphNode : graphDraw.getNodes()) {
                        if (component.tags() != null
                            && Arrays.equals(graphNode.tags(), component.tags())
                            && graphNode.type().equals(component.type())) {
                            return graphNode;
                        } else if (graphNode.type().equals(component.type())) {
                            return graphNode;
                        }
                    }

                    throw new ExtensionConfigurationException("Can't find Node with type: " + component);
                })
                .toArray(Node[]::new);

            final ApplicationGraphDraw subGraph = graphDraw.subgraph(nodesForSubGraph);
            logger.debug("@KoraAppTest subgraph took: {}", Duration.ofNanos(System.nanoTime() - startedSubgraph));

            return new GraphSupplier(() -> subGraph, meta);
        } catch (ClassNotFoundException e) {
            throw new ExtensionConfigurationException("@KoraAppTest#application must be annotated with @KoraApp, but probably wasn't: " + meta.application, e);
        } catch (Exception e) {
            throw new IllegalStateException(e);
        }
    }
}
