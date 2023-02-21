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
import ru.tinkoff.kora.annotation.processor.common.MockLifecycle;
import ru.tinkoff.kora.annotation.processor.common.TestUtils;
import ru.tinkoff.kora.application.graph.ApplicationGraphDraw;
import ru.tinkoff.kora.application.graph.Lifecycle;
import ru.tinkoff.kora.application.graph.RefreshableGraph;
import ru.tinkoff.kora.common.Component;
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

    private static final Logger logger = LoggerFactory.getLogger(KoraJUnit5Extension.class);

    static class TestClassContainer {

        final KoraAppTest koraAppTest;
        Graph graph;

        TestClassContainer(KoraAppTest koraAppTest) {
            this.koraAppTest = koraAppTest;
        }
    }

    record GraphSupplier(Supplier<? extends ApplicationGraphDraw> graphSupplier, KoraAppTest.CompilationMode shareMode) {

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
                       Class<?> aggregator,
                       List<Class<?>> classes,
                       List<Class<? extends AbstractKoraProcessor>> processors,
                       KoraAppTest.CompilationMode shareMode) {

        record Application(Class<?> real, Class<?> origin) {}
    }

    private static final ExtensionContext.Namespace NAMESPACE = ExtensionContext.Namespace.create(KoraJUnit5Extension.class);

    private static final Map<KoraAppMeta, GraphSupplier> GRAPH_SUPPLIER_MAP = new ConcurrentHashMap<>();

    private TestClassContainer getContainer(ExtensionContext context) {
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

            var graphSupplier = GRAPH_SUPPLIER_MAP.computeIfAbsent(meta, KoraJUnit5Extension::instantiateApplicationGraphSupplier);
            var graph = graphSupplier.get();
            graph.initialize();
            container.graph = graph;
        } else if (container.graph instanceof PerMethodGraph) {
            container.graph.initialize();
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
        var container = getContainer(context);
        var componentType = getParameterComponentType(parameterContext);
        return getComponent(componentType, container.graph.container()).isPresent();
    }

    @Override
    public Object resolveParameter(ParameterContext parameterContext, ExtensionContext context) throws ParameterResolutionException {
        var container = getContainer(context);
        var componentType = getParameterComponentType(parameterContext);
        return getComponent(componentType, container.graph.container())
            .orElseThrow(() -> new ExtensionConfigurationException(componentType + " expected type to be " + Lifecycle.class + ", but couldn't find it in generated Graph, please check @KoraAppTest configuration or " + componentType));
    }

    private KoraAppMeta getKoraAppMeta(KoraAppTest koraAppTest, ExtensionContext context) {
        final long started = System.nanoTime();
        var classes = Arrays.stream(koraAppTest.components())
            .distinct()
            .sorted(Comparator.comparing(Class::getCanonicalName))
            .toList();

        var processors = Stream.concat(Stream.of(KoraAppProcessor.class), Arrays.stream(koraAppTest.processors()))
            .distinct()
            .sorted(Comparator.comparing(Class::getCanonicalName))
            .toList();

        var aggregator = generateAggregatorClass(koraAppTest, context);

        final String koraAppConfig = context.getTestInstance()
            .filter(inst -> inst instanceof KoraAppTestConfigProvider)
            .map(inst -> ((KoraAppTestConfigProvider) inst).config())
            .map(String::trim)
            .orElse(koraAppTest.config().trim());

        if (koraAppConfig.isBlank()) {
            logger.info("@KoraAppTest preparation took: {}", Duration.ofNanos(System.nanoTime() - started));
            return new KoraAppMeta(new KoraAppMeta.Application(koraAppTest.application(), koraAppTest.application()),
                aggregator, classes, processors, koraAppTest.compileMode());
        }

        final KoraAppMeta.Application application = generateApplicationClass(koraAppTest, koraAppConfig, context);
        logger.info("@KoraAppTest preparation took: {}", Duration.ofNanos(System.nanoTime() - started));
        return new KoraAppMeta(application, aggregator, classes, processors, koraAppTest.compileMode());
    }

    private KoraAppMeta.Application generateApplicationClass(KoraAppTest koraAppTest, String koraAppConfig, ExtensionContext context) {
        try {
            final String className = koraAppTest.application().getPackageName() + ".$KoraAppTest_Application_" + context.getRequiredTestClass().getSimpleName();
            CtClass ctclass;
            try {
                ctclass = ClassPool.getDefault().getCtClass(className);
            } catch (NotFoundException e) {
                ctclass = ClassPool.getDefault().getCtClass(koraAppTest.application().getCanonicalName());
            }

            final Optional<CtMethod> configMethod = Arrays.stream(ctclass.getDeclaredMethods()).filter(m -> m.getName().equals("config")).findFirst();
            final String config = escape(koraAppConfig);

            ctclass.defrost();
            if (configMethod.isEmpty()) {
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
                return new KoraAppMeta.Application(applicationWithConfig, koraAppTest.application());
            } else {
                configMethod.get().setBody("return com.typesafe.config.ConfigFactory.parseString(\"%s\").resolve();".formatted(config));
                ctclass.writeFile("build/in-test-generated/classes");
                return new KoraAppMeta.Application(getClass().getClassLoader().loadClass(className), koraAppTest.application());
            }
        } catch (Exception e) {
            throw new ExtensionConfigurationException("Can't modify @KoraApp class configuration: " + koraAppTest.application(), e);
        }
    }

    private Class<?> generateAggregatorClass(KoraAppTest koraAppTest, ExtensionContext context) {
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
                return ctclass.toClass();
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
                return ctclass.toClass();
            }
        } catch (Exception e) {
            throw new ExtensionConfigurationException("Failed to created aggregator class for: " + koraAppTest.application(), e);
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
    private static GraphSupplier instantiateApplicationGraphSupplier(KoraAppMeta meta) {
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
            classAsParams.add(meta.application.real.getCanonicalName());
            classAsParams.add(meta.aggregator.getCanonicalName());

            final List<Processor> processors = meta.processors.stream()
                .map(KoraJUnit5Extension::instantiateProcessor)
                .toList();

            var classLoader = TestUtils.annotationProcessFiles(sourceClassFiles, classAsParams, true,
                p -> !p.endsWith(meta.application.real.getSimpleName() + ".class") && !p.endsWith(meta.aggregator.getSimpleName() + ".class"),
                processors);

            var clazz = classLoader.loadClass(meta.application.real.getName() + "Graph");
            var constructors = (Constructor<? extends Supplier<? extends ApplicationGraphDraw>>[]) clazz.getConstructors();
            var graphSupplier = constructors[0].newInstance();

            logger.info("@KoraAppTest compilation took: {}", Duration.ofNanos(System.nanoTime() - started));
            return new GraphSupplier(graphSupplier, meta.shareMode);
        } catch (ClassNotFoundException e) {
            throw new ExtensionConfigurationException("@KoraAppTest#application must be annotated with @KoraApp, but probably wasn't: " + meta.application.real, e);
        } catch (Exception e) {
            throw new IllegalStateException(e);
        }
    }

    private static File getSourceClassFile(Class<?> targetClass) {
        try {
            var moduleName = new File(targetClass.getProtectionDomain().getCodeSource().getLocation().getPath()).getName();
            var targetPackage = targetClass.getPackageName().replace('.', '/');
            var targetFile = targetPackage + "/" + targetClass.getSimpleName() + ".java";
            return new File("src/" + moduleName + "/java/" + targetFile);
        } catch (Exception e) {
            throw new IllegalStateException("Couldn't find '" + targetClass + "' class Source Module and failed with: " + e.getMessage());
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

    private static String escape(String s) {
        return s.replace("\\", "\\\\")
            .replace("\t", "\\t")
            .replace("\b", "\\b")
            .replace("\n", "\\n")
            .replace("\r", "\\r")
            .replace("\f", "\\f")
            .replace("\"", "\\\"");
    }
}
