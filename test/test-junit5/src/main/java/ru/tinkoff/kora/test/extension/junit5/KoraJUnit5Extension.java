package ru.tinkoff.kora.test.extension.junit5;

import com.typesafe.config.Config;
import com.typesafe.config.ConfigFactory;
import javassist.*;
import javassist.bytecode.AnnotationsAttribute;
import javassist.bytecode.annotation.Annotation;
import javassist.bytecode.annotation.StringMemberValue;
import org.jetbrains.annotations.NotNull;
import org.junit.jupiter.api.extension.*;
import org.junit.platform.commons.support.AnnotationSupport;
import org.mockito.Mockito;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import reactor.core.publisher.Mono;
import ru.tinkoff.kora.annotation.processor.common.AbstractKoraProcessor;
import ru.tinkoff.kora.annotation.processor.common.MockLifecycle;
import ru.tinkoff.kora.annotation.processor.common.TestUtils;
import ru.tinkoff.kora.application.graph.ApplicationGraphDraw;
import ru.tinkoff.kora.application.graph.Graph.Factory;
import ru.tinkoff.kora.application.graph.Lifecycle;
import ru.tinkoff.kora.application.graph.Node;
import ru.tinkoff.kora.application.graph.RefreshableGraph;
import ru.tinkoff.kora.common.Component;
import ru.tinkoff.kora.common.Tag;
import ru.tinkoff.kora.config.common.ConfigModule;
import ru.tinkoff.kora.kora.app.annotation.processor.KoraAppProcessor;
import ru.tinkoff.kora.test.extension.junit5.KoraGraphModification.NodeClassCandidate;
import ru.tinkoff.kora.test.extension.junit5.KoraGraphModification.NodeMock;

import javax.annotation.Nullable;
import javax.annotation.processing.Generated;
import javax.annotation.processing.Processor;
import java.io.File;
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

    record GraphSupplier(Supplier<? extends ApplicationGraphDraw> graphSupplier,
                         KoraAppTest.InitializeMode shareMode,
                         @Nullable KoraGraphModification graphModifier) {

        public Graph get() {
            return switch (shareMode) {
                case PER_RUN -> new PerRunGraph(graphSupplier, graphModifier);
                case PER_CLASS -> new PerClassGraph(graphSupplier, graphModifier);
                case PER_METHOD -> new PerMethodGraph(graphSupplier, graphModifier);
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
        protected final Supplier<? extends ApplicationGraphDraw> graphSupplier;
        protected volatile GraphInitialized graphInitialized;

        AbstractGraph(Supplier<? extends ApplicationGraphDraw> graphSupplier, @Nullable KoraGraphModification graphModifier) {
            this.graphSupplier = graphSupplier;
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
                    final List<Node<Object>> nodesToReplace = GraphUtils.findNodeByType(graphDraw, replacement.candidate().type(), replacement.candidate().tags());
                    if (nodesToReplace.isEmpty()) {
                        throw new ExtensionConfigurationException("Can't find Nodes to Replace: " + replacement.candidate());
                    }

                    for (Node<Object> nodeToReplace : nodesToReplace) {
                        graphDraw.replaceNode(nodeToReplace, ((Factory<Object>) getNodeFactory(replacement.function(), graphDraw)));
                    }
                }

                for (NodeMock mock : graphModifier.getMocks()) {
                    final List<Node<Object>> nodesToMock = GraphUtils.findNodeByType(graphDraw, mock.candidate().type(), mock.candidate().tags());
                    if (nodesToMock.isEmpty()) {
                        throw new ExtensionConfigurationException("Can't find Nodes to Replace: " + mock.candidate());
                    }

                    for (Node<Object> nodeToMock : nodesToMock) {
                        graphDraw.replaceNode(nodeToMock, g -> {
                            var replacement = Mockito.mock(mock.candidate().type());

                            if (Lifecycle.class.isAssignableFrom(mock.candidate().type())) {
                                Mockito.when(((Lifecycle) replacement).init()).thenReturn(Mono.empty());
                                Mockito.when(((Lifecycle) replacement).release()).thenReturn(Mono.empty());
                            }

                            return replacement;
                        });
                    }
                }

                logger.info("@KoraAppTest Graph Modification took: {}", Duration.ofNanos(System.nanoTime() - startedModify));
            }

            final long startedInit = System.nanoTime();
            final RefreshableGraph initGraph = graphDraw.init().block(Duration.ofMinutes(3));
            this.graphInitialized = new GraphInitialized(initGraph, graphDraw);
            logger.info("@KoraAppTest Graph Initialization took: {}", Duration.ofNanos(System.nanoTime() - startedInit));
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

        PerMethodGraph(Supplier<? extends ApplicationGraphDraw> graphSupplier, KoraGraphModification graphModifier) {
            super(graphSupplier, graphModifier);
        }
    }

    static class PerClassGraph extends AbstractGraph {

        public PerClassGraph(Supplier<? extends ApplicationGraphDraw> graphSupplier, KoraGraphModification graphModifier) {
            super(graphSupplier, graphModifier);
        }
    }

    static class PerRunGraph extends AbstractGraph {

        public PerRunGraph(Supplier<? extends ApplicationGraphDraw> graphSupplier, KoraGraphModification graphModifier) {
            super(graphSupplier, graphModifier);
        }

        @Override
        public void initialize() {
            if (graphInitialized == null) {
                super.initialize();
            }
        }
    }

    record GraphInitialized(RefreshableGraph refreshableGraph, ApplicationGraphDraw graphDraw) {}

    record KoraAppMeta(Application application,
                       String configuration,
                       Class<?> aggregator,
                       List<Class<?>> classes,
                       List<Class<? extends AbstractKoraProcessor>> processors,
                       KoraAppTest.InitializeMode shareMode,
                       @Nullable KoraGraphModification graphModifier) {

        record Application(Class<?> generated, Class<?> original) {}
    }

    record TestComponentCandidate(Class<?> type, Class<?>[] tags) {

        @Override
        public String toString() {
            return "[type=" + type.getCanonicalName() + ", tags=" + Arrays.toString(tags) + ']';
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
    }

    private static void injectTestComponentFields(Object o, GraphInitialized graphInitialized) {
        final List<Field> fieldsForInjection = Arrays.stream(o.getClass().getDeclaredFields())
            .filter(f -> !f.isSynthetic())
            .filter(f -> Arrays.stream(f.getDeclaredAnnotations()).anyMatch(a -> a.annotationType().equals(TestComponent.class)))
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
                .collect(Collectors.joining(", ", "Fields annotated @TestComponent have illegal 'final' modifier [", "]")));
        }

        var staticFields = fieldsForInjection.stream()
            .filter(f -> Modifier.isStatic(f.getModifiers()))
            .toList();

        if (!staticFields.isEmpty()) {
            throw new ExtensionConfigurationException(staticFields.stream()
                .map(f -> f.getDeclaringClass().getCanonicalName() + "#" + f.getName())
                .collect(Collectors.joining(", ", "Fields annotated @TestComponent have illegal 'static' modifier [", "]")));
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
            .anyMatch(a -> a.annotationType().equals(TestComponent.class));
    }

    @Override
    public Object resolveParameter(ParameterContext parameterContext, ExtensionContext context) throws ParameterResolutionException {
        var container = getContainer(context);
        var candidate = getTestComponentCandidate(parameterContext);
        return getComponentOrThrow(container.graph.initialized(), candidate);
    }

    private KoraAppMeta getKoraAppMeta(KoraAppTest koraAppTest, ExtensionContext context) {
        final long started = System.nanoTime();

        var mocks = Arrays.stream(koraAppTest.mocks())
            .map(m -> new NodeMock(new NodeClassCandidate(m, TAG_ANY)))
            .distinct()
            .sorted(Comparator.comparing(n -> n.candidate().type().getTypeName()))
            .toList();

        var processors = Stream.concat(Stream.of(KoraAppProcessor.class), Arrays.stream(koraAppTest.processors()))
            .distinct()
            .sorted(Comparator.comparing(Class::getCanonicalName))
            .collect(Collectors.toList());

        var aggregator = generateAggregatorClass(koraAppTest, context);

        final KoraGraphModification koraGraphModification = context.getTestInstance()
            .filter(inst -> inst instanceof KoraAppTestGraph)
            .map(inst -> ((KoraAppTestGraph) inst).graph())
            .map(graph -> {
                mocks.forEach(m -> graph.mockComponent(m.candidate().type(), m.candidate().tags()));
                return graph;
            })
            .orElseGet(() -> {
                if (mocks.isEmpty()) {
                    return null;
                } else {
                    var graph = KoraGraphModification.of();
                    mocks.forEach(m -> graph.mockComponent(m.candidate().type(), m.candidate().tags()));
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

        var classes = Stream.concat(Arrays.stream(koraAppTest.components()), modifierClasses)
            .distinct()
            .sorted(Comparator.comparing(Class::getCanonicalName))
            .toList();

        final String koraAppConfig = context.getTestInstance()
            .filter(inst -> inst instanceof KoraAppTestConfig)
            .map(inst -> {
                KoraConfigModification configModification = ((KoraAppTestConfig) inst).config();
                for (String configFile : koraAppTest.configFiles()) {
                    configModification.mergeWithConfigFile(configFile);
                }

                if (!koraAppTest.config().isBlank()) {
                    configModification.mergeWithConfig(koraAppTest.config());
                }

                return configModification.getConfig();
            })
            .orElseGet(() -> {
                if (koraAppTest.configFiles().length > 0) {
                    KoraConfigModification configModification = null;
                    for (String configFile : koraAppTest.configFiles()) {
                        configModification = (configModification == null)
                            ? KoraConfigModification.ofConfigFile(configFile)
                            : configModification.mergeWithConfigFile(configFile);
                    }

                    if (!koraAppTest.config().isBlank()) {
                        configModification.mergeWithConfig(koraAppTest.config());
                    }

                    return configModification.getConfig();
                } else {
                    return koraAppTest.config().trim();
                }
            });

        if (koraAppConfig.isBlank()) {
            logger.info("@KoraAppTest preparation took: {}", Duration.ofNanos(System.nanoTime() - started));
            return new KoraAppMeta(new KoraAppMeta.Application(koraAppTest.application(), koraAppTest.application()),
                koraAppConfig, aggregator, classes, processors, koraAppTest.initializeMode(), koraGraphModification);
        }

        final KoraGraphModification graphModificationWithConfig = (koraGraphModification == null)
            ? KoraGraphModification.of()
            : koraGraphModification;

        if (ConfigModule.class.isAssignableFrom(koraAppTest.application())) {
            graphModificationWithConfig.replaceComponent(() -> ConfigFactory.parseString(koraAppConfig), Config.class);
        } else {
            graphModificationWithConfig.addComponent(() -> ConfigFactory.parseString(koraAppConfig), Config.class);
        }

        logger.info("@KoraAppTest preparation took: {}", Duration.ofNanos(System.nanoTime() - started));
        return new KoraAppMeta(new KoraAppMeta.Application(koraAppTest.application(), koraAppTest.application()),
            koraAppConfig, aggregator, classes, processors, koraAppTest.initializeMode(), graphModificationWithConfig);
    }

    // Is used as aggregator for all components, so they will be constructed in graph and accessiable no matter what
    private Class<?> generateAggregatorClass(KoraAppTest koraAppTest, ExtensionContext context) {
        final long started = System.nanoTime();

        var classes = Arrays.stream(koraAppTest.components())
            .distinct()
            .sorted(Comparator.comparing(Class::getCanonicalName))
            .toList();

        final CtClass[] parameters = classes.stream()
            .map(c -> {
                try {
                    return ClassPool.getDefault().getCtClass(c.getCanonicalName());
                } catch (NotFoundException e) {
                    throw new ExtensionConfigurationException("Failed to created aggregator class, parameter class not loaded: " + c, e);
                }
            })
            .toArray(CtClass[]::new);

        try {
            final String className = koraAppTest.application().getPackageName() + ".$KoraAppTest_Aggregator_" + context.getRequiredTestClass().getSimpleName();
            CtClass ctclass;
            try {
                ctclass = ClassPool.getDefault().getCtClass(className);
                ctclass.defrost();

                for (CtConstructor constructor : ctclass.getConstructors()) {
                    ctclass.removeConstructor(constructor);
                }

                final CtConstructor constructor = CtNewConstructor.make(parameters, null, "", ctclass);
                ctclass.addConstructor(constructor);
                ctclass.writeFile("build/in-test-generated/classes");

                var result = ctclass.toClass();
                logger.debug("@KoraAppTest aggregator generation took: {}", Duration.ofNanos(System.nanoTime() - started));
                return result;
            } catch (NotFoundException e) {
                ctclass = ClassPool.getDefault().makeClass(className);
                final CtConstructor constructor = CtNewConstructor.make(parameters, null, null, ctclass);
                ctclass.addConstructor(constructor);

                var classFile = ctclass.getClassFile();
                var annotationsAttribute = new AnnotationsAttribute(classFile.getConstPool(), AnnotationsAttribute.visibleTag);
                var annotation = new Annotation(Generated.class.getCanonicalName(), classFile.getConstPool());
                annotation.addMemberValue("value", new StringMemberValue(KoraJUnit5Extension.class.getCanonicalName(), classFile.getConstPool()));
                annotationsAttribute.setAnnotation(annotation);
                classFile.addAttribute(annotationsAttribute);

                var componentAttribute = new AnnotationsAttribute(classFile.getConstPool(), AnnotationsAttribute.visibleTag);
                var component = new Annotation(Component.class.getCanonicalName(), classFile.getConstPool());
                componentAttribute.setAnnotation(component);
                classFile.addAttribute(componentAttribute);

                final CtClass lifecycle = ClassPool.getDefault().getCtClass(MockLifecycle.class.getCanonicalName());
                ctclass.addInterface(lifecycle);

                ctclass.writeFile("build/in-test-generated/classes");
                var result = ctclass.toClass();
                logger.debug("@KoraAppTest aggregator generation took: {}", Duration.ofNanos(System.nanoTime() - started));
                return result;
            }
        } catch (Exception e) {
            throw new ExtensionConfigurationException("Failed to created aggregator class for: " + koraAppTest.application(), e);
        }
    }

    private TestComponentCandidate getTestComponentCandidate(ParameterContext parameterContext) {
        final Class<?> parameterType = parameterContext.getParameter().getType();
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
                        .filter(v -> candidate.type().isAssignableFrom(v.getClass()))
                        .findFirst();
                });
        } else {
            return Optional.of(GraphUtils.findNodeByType(graph.graphDraw(), candidate.type(), candidate.tags()))
                .filter(l -> !l.isEmpty())
                .map(v -> graph.refreshableGraph().get(v.get(0)))
                .or(() -> {
                    // Try to find similar
                    return graph.graphDraw().getNodes()
                        .stream()
                        .filter(n -> Arrays.equals(n.tags(), candidate.tags()))
                        .map(graph.refreshableGraph()::get)
                        .filter(v -> candidate.type().isAssignableFrom(v.getClass()))
                        .findFirst();
                });
        }
    }

    @SuppressWarnings("unchecked")
    private static GraphSupplier generateGraphSupplier(KoraAppMeta meta) {
        try {
            final long started = System.nanoTime();

            final List<String> sourceClassFiles = meta.classes.stream()
                .map(targetClass -> {
                    try {
                        var moduleName = new File(targetClass.getProtectionDomain().getCodeSource().getLocation().getPath()).getName();
                        var targetPackage = targetClass.getPackageName().replace('.', '/');
                        var targetFile = targetPackage + "/" + targetClass.getSimpleName() + ".java";
                        var moduleFile = new File("src/" + moduleName + "/java/" + targetFile);
                        if (moduleFile.isFile()) {
                            return moduleFile.toString();
                        } else {
                            throw new IllegalStateException("Couldn't find '" + targetClass + "' class Source Module");
                        }
                    } catch (IllegalStateException e) {
                        throw e;
                    } catch (Exception e) {
                        throw new IllegalStateException("Couldn't find '" + targetClass + "' class Source Module and failed with: " + e.getMessage());
                    }
                })
                .collect(Collectors.toList());

            final List<String> generatedClassFiles = meta.classes.stream()
                .flatMap(targetClass -> {
                    try {
                        var moduleName = new File(targetClass.getProtectionDomain().getCodeSource().getLocation().getPath()).getName();
                        var targetPackage = targetClass.getPackageName().replace('.', '/');
                        var targetFile = targetPackage + "/" + targetClass.getSimpleName() + ".java";
                        var moduleFile = new File("src/" + moduleName + "/java/" + targetFile);
                        if (moduleFile.isFile()) {
                            return getGeneratedClassOnPath(targetClass, "build/classes/java/" + moduleName + "/" + targetPackage).stream();
                        } else {
                            throw new IllegalStateException("Couldn't find '" + targetClass + "' class Source Module");
                        }
                    } catch (IllegalStateException e) {
                        throw e;
                    } catch (Exception e) {
                        throw new IllegalStateException("Couldn't find '" + targetClass + "' class Source Module and failed with: " + e.getMessage());
                    }
                })
                .toList();

            var classAsParams = new ArrayList<>(generatedClassFiles);
            classAsParams.add(meta.application.generated.getCanonicalName());
            classAsParams.add(meta.aggregator.getCanonicalName());

            final List<Processor> processors = meta.processors.stream()
                .map(KoraJUnit5Extension::instantiateProcessor)
                .toList();

            var classLoader = TestUtils.annotationProcessFiles(sourceClassFiles, classAsParams, true,
                p -> !p.endsWith(meta.application.generated.getSimpleName() + ".class") && !p.endsWith(meta.aggregator.getSimpleName() + ".class"),
                processors);

            var clazz = classLoader.loadClass(meta.application.generated.getName() + "Graph");
            var constructors = (Constructor<? extends Supplier<? extends ApplicationGraphDraw>>[]) clazz.getConstructors();
            var graphSupplier = constructors[0].newInstance();
            logger.info("@KoraAppTest compilation took: {}", Duration.ofNanos(System.nanoTime() - started));
            return new GraphSupplier(graphSupplier, meta.shareMode, meta.graphModifier);
        } catch (ClassNotFoundException e) {
            throw new ExtensionConfigurationException("@KoraAppTest#application must be annotated with @KoraApp, but probably wasn't: " + meta.application.generated, e);
        } catch (Exception e) {
            throw new IllegalStateException(e);
        }
    }

    private static List<String> getGeneratedClassOnPath(Class<?> targetClass, String path) {
        var files = new File(path).listFiles();
        if (files == null) {
            return Collections.emptyList();
        }

        return Arrays.stream(files)
            .filter(file -> file.getName().startsWith("$" + targetClass.getSimpleName()))
            .map(file -> targetClass.getPackageName() + "." + file.getName().replace(".class", ""))
            .toList();
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
