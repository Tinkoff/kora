package ru.tinkoff.kora.test.extension.junit5;

import com.typesafe.config.Config;
import com.typesafe.config.ConfigFactory;
import org.junit.jupiter.api.extension.*;
import org.junit.platform.commons.support.AnnotationSupport;
import org.junit.platform.commons.util.ReflectionUtils;
import org.mockito.Mockito;
import org.mockito.internal.util.MockUtil;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import reactor.core.publisher.Mono;
import ru.tinkoff.kora.application.graph.*;
import ru.tinkoff.kora.common.Component;
import ru.tinkoff.kora.common.Tag;
import ru.tinkoff.kora.config.common.ConfigModule;
import ru.tinkoff.kora.test.extension.junit5.KoraAppTest.InitializeMode;

import javax.annotation.Nullable;
import java.lang.annotation.Annotation;
import java.lang.reflect.Constructor;
import java.lang.reflect.Field;
import java.lang.reflect.Modifier;
import java.lang.reflect.Type;
import java.time.Duration;
import java.util.*;
import java.util.concurrent.ConcurrentHashMap;
import java.util.function.Supplier;
import java.util.stream.Collectors;

final class KoraJUnit5Extension implements BeforeAllCallback, BeforeEachCallback, AfterAllCallback, AfterEachCallback, ExecutionCondition, ParameterResolver, InvocationInterceptor {

    private static final ExtensionContext.Namespace NAMESPACE = ExtensionContext.Namespace.create(KoraJUnit5Extension.class);

    private static final Logger logger = LoggerFactory.getLogger(KoraJUnit5Extension.class);

    // Application class -> graph supplier
    private static final Map<GraphSupplierKey, Supplier<ApplicationGraphDraw>> GRAPH_SUPPLIER_MAP = new ConcurrentHashMap<>();

    record GraphSupplierKey(Class<?> application, Set<GraphCandidate> components) {}

    static class GraphContainer {

        final KoraAppTest annotation;
        volatile TestGraph graph;

        GraphContainer(KoraAppTest annotation) {
            this.annotation = annotation;
        }
    }

    record KoraAppMeta(Class<?> application,
                       String configuration,
                       Set<GraphCandidate> graphRoots,
                       InitializeMode initializeMode,
                       @Nullable KoraGraphModification graphModifier) {}

    private static GraphContainer getGraphContainer(ExtensionContext context) {
        var storage = context.getStore(NAMESPACE);
        return storage.get(KoraAppTest.class, GraphContainer.class);
    }

    @Override
    public void beforeEach(ExtensionContext context) {
        var graphContainer = getGraphContainer(context);
        if (graphContainer.graph == null) {
            final KoraAppMeta meta = findKoraAppTest(context)
                .map(koraAppTest -> getKoraAppMeta(koraAppTest, context))
                .orElseThrow(() -> new ExtensionConfigurationException("@KoraAppTest not found"));

            var testGraph = KoraJUnit5Extension.generateTestGraph(meta);
            testGraph.initialize();
            graphContainer.graph = testGraph;
        }

        prepareMocks(graphContainer.graph.initialized());

        var testInstance = context.getTestInstance().orElseThrow(() -> new ExtensionConfigurationException("@KoraAppTest can't get TestInstance for @TestComponent field injection"));
        injectTestComponentFields(testInstance, graphContainer.graph.initialized());
        injectMockComponentFields(testInstance, graphContainer.graph.initialized());
        injectGraphComponentFields(testInstance, graphContainer.graph.initialized());
    }

    private static void prepareMocks(TestGraphInitialized graphInitialized) {
        for (Node<?> node : graphInitialized.graphDraw().getNodes()) {
            var mockCandidate = graphInitialized.refreshableGraph().get(node);
            if (MockUtil.isMock(mockCandidate) || MockUtil.isSpy(mockCandidate)) {
                Mockito.reset(mockCandidate);
                if (mockCandidate instanceof Lifecycle lifecycle) {
                    Mockito.when(lifecycle.init()).thenReturn(Mono.empty());
                    Mockito.when(lifecycle.release()).thenReturn(Mono.empty());
                }
            }
        }
    }

    private static void injectGraphComponentFields(Object o, TestGraphInitialized graphInitialized) {
        final List<Field> fieldsGraphForInjection = ReflectionUtils.findFields(o.getClass(), f -> !f.isSynthetic() && f.getGenericType() instanceof KoraAppGraph, ReflectionUtils.HierarchyTraversalMode.TOP_DOWN);
        for (Field field : fieldsGraphForInjection) {
            try {
                field.setAccessible(true);
                field.set(o, graphInitialized.koraAppGraph());
            } catch (Exception e) {
                throw new ExtensionConfigurationException("Failed to Inject field '" + field.getName() + "' due to: " + e);
            }
        }
    }

    private static void injectTestComponentFields(Object o, TestGraphInitialized graphInitialized) {
        injectByAnnotationFields(o, graphInitialized, TestComponent.class);
    }

    private static void injectMockComponentFields(Object o, TestGraphInitialized graphInitialized) {
        injectByAnnotationFields(o, graphInitialized, MockComponent.class);
    }

    private static void injectByAnnotationFields(Object o, TestGraphInitialized graphInitialized, Class<? extends Annotation> annotation) {
        final List<Field> fieldsForInjection = ReflectionUtils.findFields(o.getClass(), f -> !f.isSynthetic() && f.getAnnotation(annotation) != null, ReflectionUtils.HierarchyTraversalMode.TOP_DOWN);

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

            final GraphCandidate candidate = new GraphCandidate(field.getType(), tags);
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
        storage.put(KoraAppTest.class, new GraphContainer(appTest));
    }

    @Override
    public void afterAll(ExtensionContext context) {
        var graphContainer = getGraphContainer(context);
        if(graphContainer != null && graphContainer.graph.initializeMode() == InitializeMode.PER_CLASS) {
            graphContainer.graph.close();
        }
    }

    @Override
    public void afterEach(ExtensionContext context) {
        var graphContainer = getGraphContainer(context);
        if(graphContainer != null && graphContainer.graph.initializeMode() == InitializeMode.PER_METHOD) {
            graphContainer.graph.close();
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
                   .anyMatch(a -> a.annotationType().equals(TestComponent.class) || a.annotationType().equals(MockComponent.class))
               || parameterContext.getParameter().getType().equals(KoraAppGraph.class);
    }

    @Override
    public Object resolveParameter(ParameterContext parameterContext, ExtensionContext context) throws ParameterResolutionException {
        var graphContainer = getGraphContainer(context);
        var graphCandidate = getGraphCandidate(parameterContext);
        return getComponentOrThrow(graphContainer.graph.initialized(), graphCandidate);
    }

    private KoraAppMeta getKoraAppMeta(KoraAppTest koraAppTest, ExtensionContext context) {
        final long started = System.nanoTime();

        var testInstance = context.getTestInstance().orElseThrow(() -> new ExtensionConfigurationException("@KoraAppTest can't get TestInstance for @TestComponent field injection"));
        var mockComponentFromFields = ReflectionUtils.findFields(testInstance.getClass(), f -> !f.isSynthetic() && f.getAnnotation(MockComponent.class) != null, ReflectionUtils.HierarchyTraversalMode.TOP_DOWN)
            .stream()
            .map(field -> {
                final Class<?>[] tag = Optional.ofNullable(field.getAnnotation(Tag.class))
                    .map(Tag::value)
                    .orElse(null);

                return new GraphMock(new GraphCandidate(field.getGenericType(), tag));
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

                    return new GraphMock(new GraphCandidate(parameter.getParameterizedType(), tag));
                })
                .toList())
            .orElse(List.of());

        final KoraGraphModification koraGraphModification = context.getTestInstance()
            .filter(inst -> inst instanceof KoraAppTestGraphModifier)
            .map(inst -> ((KoraAppTestGraphModifier) inst).graph())
            .map(graph -> {
                mockComponentFromFields.forEach(m -> graph.mockComponent(m.candidate().type(), m.candidate().tags()));
                mockComponentFromParameters.forEach(m -> graph.mockComponent(m.candidate().type(), m.candidate().tags()));
                return graph;
            })
            .orElseGet(() -> {
                if (mockComponentFromFields.isEmpty() && mockComponentFromParameters.isEmpty()) {
                    return null;
                } else {
                    var graph = KoraGraphModification.create();
                    mockComponentFromFields.forEach(m -> graph.mockComponent(m.candidate().type(), m.candidate().tags()));
                    mockComponentFromParameters.forEach(m -> graph.mockComponent(m.candidate().type(), m.candidate().tags()));
                    return graph;
                }
            });

        final Set<GraphCandidate> graphRoots = Arrays.stream(koraAppTest.components())
            .map(GraphCandidate::new)
            .collect(Collectors.toSet());

        final String koraAppConfig = context.getTestInstance()
            .filter(inst -> inst instanceof KoraAppTestConfigModifier)
            .map(inst -> {
                final KoraConfigModification configModification = ((KoraAppTestConfigModifier) inst).config();
                return configModification.getConfig();
            })
            .orElse("");

        if (koraAppConfig.isBlank()) {
            logger.debug("@KoraAppTest preparation took: {}", Duration.ofNanos(System.nanoTime() - started));
            return new KoraAppMeta(koraAppTest.value(), koraAppConfig,
                graphRoots, koraAppTest.initializeMode(), koraGraphModification);
        }

        final KoraGraphModification graphModificationWithConfig = (koraGraphModification == null)
            ? KoraGraphModification.create()
            : koraGraphModification;

        if (ConfigModule.class.isAssignableFrom(koraAppTest.value())) {
            graphModificationWithConfig.replaceComponent(Config.class, () -> ConfigFactory.parseString(koraAppConfig));
        } else {
            graphModificationWithConfig.addComponent(Config.class, () -> ConfigFactory.parseString(koraAppConfig));
        }

        logger.debug("@KoraAppTest preparation took: {}", Duration.ofNanos(System.nanoTime() - started));
        return new KoraAppMeta(koraAppTest.value(), koraAppConfig,
            graphRoots, koraAppTest.initializeMode(), graphModificationWithConfig);
    }

    private static GraphCandidate getGraphCandidate(ParameterContext parameterContext) {
        final Type parameterType = parameterContext.getParameter().getParameterizedType();
        final Class<?>[] tags = Arrays.stream(parameterContext.getParameter().getDeclaredAnnotations())
            .filter(a -> a.annotationType().equals(Tag.class))
            .map(a -> ((Tag) a).value())
            .findFirst()
            .orElse(null);

        return new GraphCandidate(parameterType, tags);
    }

    private static Object getComponentOrThrow(TestGraphInitialized graphInitialized, GraphCandidate candidate) {
        return getComponent(graphInitialized, candidate)
            .orElseThrow(() -> new ExtensionConfigurationException(candidate + " expected type to implement " + Lifecycle.class + " or be a " + Component.class
                                                                   + ", but it was not present generated graph, please check @KoraAppTest configuration for " + candidate));
    }

    private static Optional<Object> getComponent(TestGraphInitialized graphInitialized, GraphCandidate candidate) {
        try {
            return getComponentFromGraph(graphInitialized, candidate);
        } catch (Exception e) {
            logger.warn(e.getMessage());
            return Optional.empty();
        }
    }

    private static Optional<Object> getComponentFromGraph(TestGraphInitialized graph, GraphCandidate candidate) {
        if(KoraAppGraph.class.equals(candidate.type())) {
            return Optional.of(graph.koraAppGraph());
        }

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
    private static TestGraph generateTestGraph(KoraAppMeta meta) {
        var graphSupplier = GRAPH_SUPPLIER_MAP.computeIfAbsent(new GraphSupplierKey(meta.application(), meta.graphRoots()), k -> {
            try {
                final long startedLoading = System.nanoTime();
                var clazz = KoraJUnit5Extension.class.getClassLoader().loadClass(meta.application.getName() + "Graph");
                var constructors = (Constructor<? extends Supplier<? extends ApplicationGraphDraw>>[]) clazz.getConstructors();
                var supplier = (Supplier<ApplicationGraphDraw>) constructors[0].newInstance();
                logger.debug("@KoraAppTest loading took: {}", Duration.ofNanos(System.nanoTime() - startedLoading));
                return supplier;
            } catch (ClassNotFoundException e) {
                throw new ExtensionConfigurationException("@KoraAppTest#value must be annotated with @KoraApp, but probably wasn't: " + meta.application, e);
            } catch (Exception e) {
                throw new IllegalStateException(e);
            }
        });

        final long startedSubgraph = System.nanoTime();
        final ApplicationGraphDraw graphDraw = graphSupplier.get();
        final Node<?>[] nodesForSubGraph = meta.graphRoots.stream()
            .flatMap(component -> {
                final Set<Node<Object>> nodes = GraphUtils.findNodeByTypeOrAssignable(graphDraw, component);
                if (nodes.isEmpty()) {
                    throw new ExtensionConfigurationException("Can't find Node with type: " + component);
                }

                return nodes.stream();
            })
            .toArray(Node[]::new);

        final ApplicationGraphDraw subGraph = (nodesForSubGraph.length == 0)
            ? graphDraw
            : graphDraw.subgraph(nodesForSubGraph);
        logger.debug("@KoraAppTest subgraph took: {}", Duration.ofNanos(System.nanoTime() - startedSubgraph));
        return new TestGraph(subGraph::copy, meta.graphModifier(), meta.initializeMode);
    }
}
