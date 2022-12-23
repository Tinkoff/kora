package ru.tinkoff.kora.test.extension.junit5;

import javassist.*;
import javassist.bytecode.AnnotationsAttribute;
import javassist.bytecode.annotation.Annotation;
import javassist.bytecode.annotation.StringMemberValue;
import org.junit.jupiter.api.extension.*;
import org.junit.platform.commons.support.AnnotationSupport;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import ru.tinkoff.kora.annotation.processor.common.AbstractKoraProcessor;
import ru.tinkoff.kora.annotation.processor.common.TestUtils;
import ru.tinkoff.kora.application.graph.ApplicationGraphDraw;
import ru.tinkoff.kora.application.graph.Lifecycle;
import ru.tinkoff.kora.application.graph.RefreshableGraph;
import ru.tinkoff.kora.config.common.ConfigModule;
import ru.tinkoff.kora.kora.app.annotation.processor.KoraAppProcessor;

import javax.annotation.processing.Generated;
import javax.annotation.processing.Processor;
import java.io.File;
import java.lang.reflect.Constructor;
import java.lang.reflect.Executable;
import java.time.Duration;
import java.util.*;
import java.util.concurrent.ConcurrentHashMap;
import java.util.function.Supplier;
import java.util.stream.Collectors;
import java.util.stream.Stream;

public class KoraJUnit5Extension implements BeforeAllCallback, BeforeEachCallback, ExecutionCondition, ParameterResolver, InvocationInterceptor {

    record GraphSupplier(Supplier<? extends ApplicationGraphDraw> graphSupplier, KoraAppTest.CompilationShareMode shareMode) {

        public Graph get() {
            return switch (shareMode) {
                case PER_RUN -> new PerRunGraph(graphSupplier);
                case PER_CLASS -> new PerClassGraph(graphSupplier);
                case PER_METHOD -> new PerMethodGraph(graphSupplier);
            };
        }
    }

    interface Graph {

        void initialize();

        InitializedGraph container();
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
            logger.info("@KoraAppTest Graph Initialization took: {}", Duration.ofNanos(System.nanoTime() - started));
        }

        @Override
        public InitializedGraph container() {
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

    record KoraAppMeta(Application application,
                       List<Class<? extends Lifecycle>> classes,
                       List<Class<? extends AbstractKoraProcessor>> processors,
                       KoraAppTest.CompilationShareMode shareMode) {

        record Application(Class<?> real, Class<?> origin) {}
    }

    private static final ExtensionContext.Namespace NAMESPACE = ExtensionContext.Namespace.create(KoraJUnit5Extension.class);

    private static final Map<Class<?>, Processor> PROCESSOR_INSTANCES = new ConcurrentHashMap<>();
    private static final Map<KoraAppMeta, GraphSupplier> GRAPH_SUPPLIER_MAP = new ConcurrentHashMap<>();

    private Graph getGraph(ExtensionContext context) {
        var storage = context.getStore(NAMESPACE);
        return storage.get(Graph.class, Graph.class);
    }

    @Override
    public void beforeEach(ExtensionContext context) {
        var graph = getGraph(context);
        if (graph instanceof PerMethodGraph) {
            graph.initialize();
        }
    }

    @Override
    public void beforeAll(ExtensionContext context) {
        final KoraAppMeta meta = findKoraAppTest(context)
            .map(koraAppTest -> getKoraAppMeta(koraAppTest, context))
            .orElseThrow(() -> new ExtensionConfigurationException("@KoraAppTest not found"));

        var storage = context.getStore(NAMESPACE);
        var graphSupplier = GRAPH_SUPPLIER_MAP.computeIfAbsent(meta, KoraJUnit5Extension::getApplicationGraphSupplier);
        var graph = graphSupplier.get();
        storage.put(KoraAppTest.class, meta);
        storage.put(Graph.class, graph);

        if (graph instanceof PerClassGraph || graph instanceof PerRunGraph) {
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
        var componentType = getParameterComponentType(parameterContext);
        return getComponent(componentType, graph.container()).isPresent();
    }

    @Override
    public Object resolveParameter(ParameterContext parameterContext, ExtensionContext context) throws ParameterResolutionException {
        var graph = getGraph(context);
        var componentType = getParameterComponentType(parameterContext);
        return getComponent(componentType, graph.container())
            .orElseThrow(() -> new ExtensionConfigurationException(componentType + " expected type to be " + Lifecycle.class + ", but couldn't find it in generated Graph, please check @KoraAppTest configuration or " + componentType));
    }

    private KoraAppMeta getKoraAppMeta(KoraAppTest koraAppTest, ExtensionContext context) {
        var classes = Arrays.stream(koraAppTest.classes())
            .distinct()
            .sorted(Comparator.comparing(Class::getCanonicalName))
            .toList();

        var processors = Stream.concat(Arrays.stream(koraAppTest.processors()), Stream.of(KoraAppProcessor.class))
            .distinct()
            .sorted(Comparator.comparing(Class::getCanonicalName))
            .toList();

        if (koraAppTest.config().isBlank()) {
            return new KoraAppMeta(new KoraAppMeta.Application(koraAppTest.application(), koraAppTest.application()), classes, processors, koraAppTest.shareMode());
        }

        try {
            final String className = koraAppTest.application().getPackageName() + ".$KoraAppTest_" + context.getRequiredTestClass().getSimpleName() + "_" + koraAppTest.application().getSimpleName();
            CtClass ctclass;
            try {
                ctclass = ClassPool.getDefault().getCtClass(className);
            } catch (NotFoundException e) {
                ctclass = ClassPool.getDefault().getCtClass(koraAppTest.application().getCanonicalName());
            }

            final Optional<CtMethod> configMethod = Arrays.stream(ctclass.getDeclaredMethods()).filter(m -> m.getName().equals("config")).findFirst();
            final String config = escape(koraAppTest.config().trim());
            if (configMethod.isEmpty()) {
                ctclass.defrost();
                if (Arrays.stream(koraAppTest.application().getInterfaces()).noneMatch(i -> i.equals(ConfigModule.class))) {
                    ctclass.addInterface(ClassPool.getDefault().get(ConfigModule.class.getCanonicalName()));
                }

                final CtMethod method = CtNewMethod.make("public com.typesafe.config.Config config() { return com.typesafe.config.ConfigFactory.parseString(\"%s\").resolve(); }".formatted(config), ctclass);
                var methodConstPool = method.getMethodInfo().getConstPool();
                var methodAnnotationsAttribute = new AnnotationsAttribute(methodConstPool, AnnotationsAttribute.visibleTag);
                var methodAnnotation = new Annotation(Override.class.getCanonicalName(), methodConstPool);
                methodAnnotationsAttribute.setAnnotation(methodAnnotation);
                method.getMethodInfo().addAttribute(methodAnnotationsAttribute);

                ctclass.addMethod(method);
                ctclass.setName(className);

                var classFile = ctclass.getClassFile();
                var annotationsAttribute = new AnnotationsAttribute(classFile.getConstPool(), AnnotationsAttribute.visibleTag);
                var annotation = new Annotation(Generated.class.getCanonicalName(), classFile.getConstPool());
                annotation.addMemberValue("value", new StringMemberValue(KoraJUnit5Extension.class.getCanonicalName(), classFile.getConstPool()));
                annotationsAttribute.setAnnotation(annotation);
                classFile.addAttribute(annotationsAttribute);
                ctclass.writeFile("build/in-test-generated/classes");

                final Class<?> applicationWithConfig = ctclass.toClass();
                var application = new KoraAppMeta.Application(applicationWithConfig, koraAppTest.application());
                return new KoraAppMeta(application, classes, processors, koraAppTest.shareMode());
            } else {
                ctclass.defrost();
                configMethod.get().setBody("return com.typesafe.config.ConfigFactory.parseString(\"%s\").resolve();".formatted(config));
                ctclass.writeFile("build/in-test-generated/classes");
                var application = new KoraAppMeta.Application(getClass().getClassLoader().loadClass(className), koraAppTest.application());
                return new KoraAppMeta(application, classes, processors, koraAppTest.shareMode());
            }
        } catch (Exception e) {
            throw new ExtensionConfigurationException("Can't modify @KoraApp class configuration: " + koraAppTest.application(), e);
        }
    }

    private Class<?> getParameterComponentType(ParameterContext parameterContext) {
        final Executable declaringExecutable = parameterContext.getDeclaringExecutable();
        final int index = parameterContext.getIndex();
        return declaringExecutable.getParameterTypes()[index];
    }

    private Optional<? extends Object> getComponent(Class<?> componentType, InitializedGraph initializedGraph) {
        try {
            return getComponentFromGraph(initializedGraph, componentType);
        } catch (Exception e) {
            return Optional.empty();
        }
    }

    @SuppressWarnings("unchecked")
    private static <T> Optional<T> getComponentFromGraph(InitializedGraph graph, Class<T> targetType) {
        var values = graph.graphDraw().getNodes()
            .stream()
            .map(graph.refreshableGraph()::get)
            .toList();

        return values.stream()
            .filter(a -> targetType.isAssignableFrom(a.getClass()))
            .map(a -> ((T) a))
            .findFirst();
    }

    @SuppressWarnings("unchecked")
    private static GraphSupplier getApplicationGraphSupplier(KoraAppMeta meta) {
        try {
            final List<String> classesAsFiles = meta.classes.stream()
                .map(targetClass -> {
                    var targetFile = targetClass.getName().replace('.', '/') + ".java";
                    var rootTest = "src/test/java/";
                    var rootMain = "src/main/java/";
                    var rootInTests = "build/in-test-generated/sources";
                    var testFile = new File(rootTest + targetFile);
                    var mainFile = new File(rootMain + targetFile);
                    var inTestsFile = new File(rootInTests + targetFile);
                    if(testFile.isFile()) {
                        return rootTest + targetFile;
                    } else if(mainFile.isFile()) {
                        return rootMain + targetFile;
                    } else if(inTestsFile.isFile()) {
                        return rootMain + targetFile;
                    } else {
                        throw new IllegalStateException("Can't find class in main or test directory: " + targetClass);
                    }
                })
                .collect(Collectors.toList());

            final List<Processor> processors = meta.processors.stream()
                .map(p -> PROCESSOR_INSTANCES.computeIfAbsent(p, (k) -> instantiateProcessor(p)))
                .toList();

            var classLoader = TestUtils.annotationProcessFiles(classesAsFiles, List.of(meta.application.real.getCanonicalName()), true, p -> !p.endsWith(meta.application.real.getSimpleName() + ".class"), processors);
            var clazz = classLoader.loadClass(meta.application.real.getName() + "Graph");
            var constructors = (Constructor<? extends Supplier<? extends ApplicationGraphDraw>>[]) clazz.getConstructors();
            var graphSupplier = constructors[0].newInstance();

            return new GraphSupplier(graphSupplier, meta.shareMode);
        } catch (ClassNotFoundException e) {
            throw new ExtensionConfigurationException("@KoraAppTest#application must be annotated with @KoraApp, but probably wasn't: " + meta.application.real, e);
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

    private static String escape(String s){
        return s.replace("\\", "\\\\")
            .replace("\t", "\\t")
            .replace("\b", "\\b")
            .replace("\n", "\\n")
            .replace("\r", "\\r")
            .replace("\f", "\\f")
            .replace("\"", "\\\"");
    }
}
