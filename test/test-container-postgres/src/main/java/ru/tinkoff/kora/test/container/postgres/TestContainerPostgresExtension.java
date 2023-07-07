package ru.tinkoff.kora.test.container.postgres;

import liquibase.Contexts;
import liquibase.LabelExpression;
import liquibase.Liquibase;
import liquibase.database.DatabaseFactory;
import liquibase.database.jvm.JdbcConnection;
import liquibase.exception.LiquibaseException;
import liquibase.resource.ClassLoaderResourceAccessor;
import org.flywaydb.core.Flyway;
import org.junit.jupiter.api.extension.*;
import org.junit.platform.commons.support.AnnotationSupport;
import org.junit.platform.commons.util.ReflectionUtils;
import org.junit.platform.launcher.TestExecutionListener;
import org.junit.platform.launcher.TestPlan;
import org.slf4j.LoggerFactory;
import org.testcontainers.containers.Network;
import org.testcontainers.containers.PostgreSQLContainer;
import org.testcontainers.containers.output.Slf4jLogConsumer;
import org.testcontainers.containers.wait.strategy.Wait;
import org.testcontainers.utility.DockerImageName;
import ru.tinkoff.kora.test.container.postgres.TestcontainersPostgres.ExecuteMode;
import ru.tinkoff.kora.test.container.postgres.TestcontainersPostgres.MigrationEngine;
import ru.tinkoff.kora.test.container.postgres.TestcontainersPostgres.StartMode;

import javax.annotation.Nullable;
import java.io.FileWriter;
import java.io.Writer;
import java.lang.reflect.Field;
import java.lang.reflect.Method;
import java.lang.reflect.Modifier;
import java.nio.file.Files;
import java.util.Arrays;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.concurrent.ConcurrentHashMap;

final class TestContainerPostgresExtension implements BeforeAllCallback, BeforeEachCallback, AfterAllCallback, AfterEachCallback, TestExecutionListener, ParameterResolver {

    private static final ExtensionContext.Namespace NAMESPACE = ExtensionContext.Namespace.create(TestContainerPostgresExtension.class);

    private static final Map<String, ExtensionContainer> IMAGE_TO_SHARED_CONTAINER = new ConcurrentHashMap<>();
    private static volatile PostgresConnection externalConnection = null;
    private static volatile TestcontainersPostgres externalDropAnnotation = null;

    record ExtensionContainer(PostgreSQLContainer<?> container, PostgresConnection connection) {}

    @SuppressWarnings("unchecked")
    private static PostgreSQLContainer<?> getContainer(String image, ExtensionContext context) {
        return ReflectionUtils.findFields(context.getRequiredTestClass(),
                f -> !f.isSynthetic() && f.getAnnotation(ContainerPostgres.class) != null,
                ReflectionUtils.HierarchyTraversalMode.TOP_DOWN)
            .stream()
            .findFirst()
            .flatMap(field -> context.getTestInstance()
                .map(instance -> {
                    try {
                        Object possibleContainer = field.get(instance);
                        if (possibleContainer instanceof PostgreSQLContainer<?> pc) {
                            return pc;
                        } else {
                            throw new IllegalArgumentException("Field '%s' annotated with @%s value must be instance of %s".formatted(
                                field.getName(), ContainerPostgres.class.getSimpleName(), PostgreSQLContainer.class
                            ));
                        }
                    } catch (IllegalAccessException e) {
                        throw new IllegalStateException("Failed retrieving value from field '%s' annotated with @%s"
                            .formatted(field.getName(), ContainerPostgres.class.getSimpleName()), e);
                    }
                }))
            .orElseGet(() -> {
                var dockerImage = DockerImageName.parse(image).asCompatibleSubstituteFor(DockerImageName.parse(PostgreSQLContainer.IMAGE));
                return (PostgreSQLContainer) new PostgreSQLContainer<>(dockerImage)
                    .withDatabaseName("postgres")
                    .withUsername("postgres")
                    .withPassword("postgres")
                    .withLogConsumer(new Slf4jLogConsumer(LoggerFactory.getLogger(PostgreSQLContainer.class)).withMdc("image", image))
                    .withNetwork(Network.SHARED)
                    .waitingFor(Wait.forListeningPort());
            });
    }

    private static Flyway getFlyway(PostgresConnection connection, List<String> locations) {
        final List<String> migrationLocations = (locations.isEmpty())
            ? List.of("classpath:db/migration")
            : locations;

        return Flyway.configure()
            .dataSource(connection.jdbcUrl(), connection.username(), connection.password())
            .locations(migrationLocations.toArray(String[]::new))
            .cleanDisabled(false)
            .load();
    }

    private static void migrateFlyway(PostgresConnection connection, List<String> locations) {
        getFlyway(connection, locations).migrate();
    }

    private static void dropFlyway(PostgresConnection connection, List<String> locations) {
        getFlyway(connection, locations).clean();
    }

    @FunctionalInterface
    interface LiquibaseRunner {
        void apply(Liquibase liquibase, Writer writer) throws LiquibaseException;
    }

    private static void prepareLiquibase(PostgresConnection connection, List<String> locations, LiquibaseRunner liquibaseConsumer) {
        try {
            final List<String> changeLogLocations = (locations.isEmpty())
                ? List.of("db/changelog.sql")
                : locations;

            try (var con = connection.open()) {
                var liquibaseConnection = new JdbcConnection(con);
                var database = DatabaseFactory.getInstance().findCorrectDatabaseImplementation(liquibaseConnection);
                for (String changeLog : changeLogLocations) {
                    try (var classLoaderResourceAccessor = new ClassLoaderResourceAccessor()) {
                        try (var liquibase = new Liquibase(changeLog, classLoaderResourceAccessor, database)) {
                            var tmpFile = Files.createTempFile("liquibase-changelog-output", ".txt");
                            try (var writer = new FileWriter(tmpFile.toFile())) {
                                liquibaseConsumer.apply(liquibase, writer);
                            }
                        }
                    }
                }
            }
        } catch (Exception e) {
            throw new IllegalStateException(e);
        }
    }

    private static void migrateLiquibase(PostgresConnection connection, List<String> locations) {
        prepareLiquibase(connection, locations, (liquibase, writer) -> {
            var contexts = new Contexts();
            var labelExpression = new LabelExpression();
            var changeSetStatuses = liquibase.getChangeSetStatuses(contexts, labelExpression, true);
            if (!changeSetStatuses.isEmpty()) {
                liquibase.update();
            }
        });
    }

    private static void dropLiquibase(PostgresConnection connection, List<String> locations) {
        prepareLiquibase(connection, locations, (liquibase, writer) -> liquibase.dropAll());
    }

    private static void tryMigrateIfRequired(TestcontainersPostgres annotation, PostgresConnection postgresConnection) {
        if (annotation.migration().engine() == MigrationEngine.FLYWAY) {
            migrateFlyway(postgresConnection, Arrays.asList(annotation.migration().migrationPaths()));
        } else if (annotation.migration().engine() == MigrationEngine.LIQUIBASE) {
            migrateLiquibase(postgresConnection, Arrays.asList(annotation.migration().migrationPaths()));
        }
    }

    private static void tryDropIfRequired(TestcontainersPostgres annotation, PostgresConnection postgresConnection) {
        if (annotation.migration().engine() == MigrationEngine.FLYWAY) {
            dropFlyway(postgresConnection, Arrays.asList(annotation.migration().migrationPaths()));
        } else if (annotation.migration().engine() == MigrationEngine.LIQUIBASE) {
            dropLiquibase(postgresConnection, Arrays.asList(annotation.migration().migrationPaths()));
        }
    }

    private static void injectPostgresConnection(PostgresConnection connection, ExtensionContext context) {
        var connectionFields = ReflectionUtils.findFields(context.getRequiredTestClass(),
            f -> !f.isSynthetic()
                 && !Modifier.isFinal(f.getModifiers())
                 && !Modifier.isStatic(f.getModifiers())
                 && f.getAnnotation(ContainerPostgresConnection.class) != null,
            ReflectionUtils.HierarchyTraversalMode.TOP_DOWN);

        context.getTestInstance().ifPresent(instance -> {
            for (Field field : connectionFields) {
                try {
                    field.setAccessible(true);
                    field.set(instance, connection);
                } catch (IllegalAccessException e) {
                    throw new IllegalStateException("Field '%s' annotated with @%s can't set connection".formatted(
                        field.getName(), ContainerPostgresConnection.class.getSimpleName()
                    ), e);
                }
            }
        });
    }

    @Override
    public void testPlanExecutionStarted(TestPlan testPlan) {
        externalConnection = getPostgresConnection();
    }

    @Override
    public void beforeAll(ExtensionContext context) throws Exception {
        final TestcontainersPostgres annotation = findAnnotation(context)
            .orElseThrow(() -> new ExtensionConfigurationException("@TestContainerPostgres not found"));

        if (externalConnection != null) {
            if (annotation.migration().apply() == ExecuteMode.PER_CLASS) {
                tryMigrateIfRequired(annotation, externalConnection);
            }

            return;
        }

        var storage = context.getStore(NAMESPACE);
        if (annotation.startMode() == StartMode.PER_RUN) {
            var extensionContainer = IMAGE_TO_SHARED_CONTAINER.computeIfAbsent(annotation.image(), k -> {
                var container = getContainer(annotation.image(), context);
                container.start();
                container.withReuse(true);
                var postgresConnection = new PostgresConnectionImpl(container);
                return new ExtensionContainer(container, postgresConnection);
            });

            storage.put(PostgresConnection.class, extensionContainer.connection());

            if (annotation.migration().apply() == ExecuteMode.PER_CLASS) {
                tryMigrateIfRequired(annotation, extensionContainer.connection());
            }
        } else if (annotation.startMode() == StartMode.PER_CLASS) {
            var container = getContainer(annotation.image(), context);
            container.start();
            var postgresConnection = new PostgresConnectionImpl(container);
            var extensionContainer = new ExtensionContainer(container, postgresConnection);
            storage.put(StartMode.PER_CLASS, extensionContainer);
            storage.put(PostgresConnection.class, postgresConnection);

            if (annotation.migration().apply() == ExecuteMode.PER_CLASS) {
                tryMigrateIfRequired(annotation, postgresConnection);
            }
        }
    }

    @Override
    public void beforeEach(ExtensionContext context) throws Exception {
        final TestcontainersPostgres annotation = findAnnotation(context)
            .orElseThrow(() -> new ExtensionConfigurationException("@TestContainerPostgres not found"));

        if (externalConnection != null) {
            if (annotation.migration().apply() == ExecuteMode.PER_METHOD) {
                tryMigrateIfRequired(annotation, externalConnection);
            }

            injectPostgresConnection(externalConnection, context);
            return;
        }

        var storage = context.getStore(NAMESPACE);
        if (annotation.startMode() == StartMode.PER_METHOD) {
            var container = getContainer(annotation.image(), context);
            container.start();
            var postgresConnection = new PostgresConnectionImpl(container);

            if (annotation.migration().apply() == ExecuteMode.PER_METHOD) {
                tryMigrateIfRequired(annotation, postgresConnection);
            }

            injectPostgresConnection(postgresConnection, context);
            storage.put(PostgresConnection.class, postgresConnection);
            storage.put(StartMode.PER_METHOD, new ExtensionContainer(container, postgresConnection));
        } else {
            var postgresConnection = storage.get(PostgresConnection.class, PostgresConnection.class);
            injectPostgresConnection(postgresConnection, context);

            if (annotation.migration().apply() == ExecuteMode.PER_METHOD) {
                tryMigrateIfRequired(annotation, postgresConnection);
            }
        }
    }

    @Override
    public void afterEach(ExtensionContext context) throws Exception {
        final TestcontainersPostgres annotation = findAnnotation(context)
            .orElseThrow(() -> new ExtensionConfigurationException("@TestContainerPostgres not found"));

        if (externalConnection != null) {
            if (annotation.migration().drop() == ExecuteMode.PER_METHOD) {
                tryDropIfRequired(annotation, externalConnection);
            }

            return;
        }

        var storage = context.getStore(NAMESPACE);
        if (annotation.startMode() == StartMode.PER_METHOD) {
            var extensionContainer = storage.get(StartMode.PER_METHOD, ExtensionContainer.class);
            extensionContainer.container().stop();
        } else if (annotation.startMode() == StartMode.PER_CLASS) {
            var extensionContainer = storage.get(StartMode.PER_CLASS, ExtensionContainer.class);
            if (annotation.migration().drop() == ExecuteMode.PER_METHOD) {
                tryDropIfRequired(annotation, extensionContainer.connection());
            }
        } else if (annotation.startMode() == StartMode.PER_RUN) {
            Optional.ofNullable(IMAGE_TO_SHARED_CONTAINER.get(annotation.image())).ifPresent(extensionContainer -> {
                if (annotation.migration().drop() == ExecuteMode.PER_METHOD) {
                    tryDropIfRequired(annotation, extensionContainer.connection());
                }
            });
        }
    }

    @Override
    public void afterAll(ExtensionContext context) throws Exception {
        final TestcontainersPostgres annotation = findAnnotation(context)
            .orElseThrow(() -> new ExtensionConfigurationException("@TestContainerPostgres not found"));

        if (externalConnection != null) {
            if (annotation.migration().drop() == ExecuteMode.PER_CLASS) {
                tryDropIfRequired(annotation, externalConnection);
            }

            return;
        }

        var storage = context.getStore(NAMESPACE);
        if (annotation.startMode() == StartMode.PER_CLASS) {
            var extensionContainer = storage.get(StartMode.PER_CLASS, ExtensionContainer.class);
            extensionContainer.container().stop();
        } else if (annotation.startMode() == StartMode.PER_RUN) {
            Optional.ofNullable(IMAGE_TO_SHARED_CONTAINER.get(annotation.image())).ifPresent(extensionContainer -> {
                if (annotation.migration().drop() == ExecuteMode.PER_CLASS) {
                    tryDropIfRequired(annotation, extensionContainer.connection());
                }
            });
        }
    }

    @Override
    public void testPlanExecutionFinished(TestPlan testPlan) {
        if (externalConnection != null && externalDropAnnotation != null) {
            tryDropIfRequired(externalDropAnnotation, externalConnection);
            return;
        }

        for (ExtensionContainer container : IMAGE_TO_SHARED_CONTAINER.values()) {
            container.container().stop();
        }
    }

    private static Optional<TestcontainersPostgres> findAnnotation(ExtensionContext context) {
        Optional<ExtensionContext> current = Optional.of(context);
        while (current.isPresent()) {
            final Optional<TestcontainersPostgres> annotation = AnnotationSupport.findAnnotation(current.get().getRequiredTestClass(), TestcontainersPostgres.class);
            if (annotation.isPresent()) {
                return annotation;
            }

            current = current.get().getParent();
        }

        return Optional.empty();
    }

    @Nullable
    private static PostgresConnection getPostgresConnection() {
        var host = System.getenv("TEST_POSTGRES_HOST");
        if (host == null) {
            return null;
        }

        var port = System.getenv("TEST_POSTGRES_PORT");
        if (port == null) {
            return null;
        }

        var db = Optional.ofNullable(System.getenv("TEST_POSTGRES_DB")).orElse("postgres");
        var user = Optional.ofNullable(System.getenv("TEST_POSTGRES_USERNAME")).orElse("postgres");
        var password = Optional.ofNullable(System.getenv("TEST_POSTGRES_PASSWORD")).orElse("postgres");
        return new PostgresConnectionImpl(host, Integer.parseInt(port), db, user, password);
    }

    @Override
    public boolean supportsParameter(ParameterContext parameterContext, ExtensionContext extensionContext) throws ParameterResolutionException {
        final boolean foundSuitable = parameterContext.getDeclaringExecutable() instanceof Method
                                      && parameterContext.getParameter().getAnnotation(ContainerPostgresConnection.class) != null;

        if (!foundSuitable) {
            return false;
        }

        if (!parameterContext.getParameter().getType().equals(PostgresConnection.class)) {
            throw new ExtensionConfigurationException("Parameter '%s' annotated @%s is not of type %s".formatted(
                parameterContext.getParameter().getName(), ContainerPostgresConnection.class.getSimpleName(), PostgresConnection.class
            ));
        }

        return true;
    }

    @Override
    public Object resolveParameter(ParameterContext parameterContext, ExtensionContext extensionContext) throws ParameterResolutionException {
        if (externalConnection != null) {
            return externalConnection;
        }

        var storage = extensionContext.getStore(NAMESPACE);
        return storage.get(PostgresConnection.class, PostgresConnection.class);
    }
}
