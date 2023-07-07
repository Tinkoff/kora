package ru.tinkoff.kora.test.container.postgres;

import org.junit.jupiter.api.Order;
import org.junit.jupiter.api.extension.ExtendWith;

import java.lang.annotation.*;

@Order(Order.DEFAULT - 100) // Run before other extensions
@ExtendWith(TestContainerPostgresExtension.class)
@Documented
@Target({ElementType.TYPE})
@Retention(RetentionPolicy.RUNTIME)
public @interface TestcontainersPostgres {

    enum StartMode {
        PER_RUN,
        PER_CLASS,
        PER_METHOD
    }

    enum ExecuteMode {
        NONE,
        PER_CLASS,
        PER_METHOD
    }

    enum MigrationEngine {
        FLYWAY,
        LIQUIBASE
    }

    @Documented
    @Target({})
    @Retention(RetentionPolicy.RUNTIME)
    @interface Migration {

        MigrationEngine engine();

        ExecuteMode apply();

        ExecuteMode drop();

        /**
         * @return will be by default "classpath:db/migration" for FlyWay and "db/migration/changelog.sql" for Liquibase
         */
        String[] migrationPaths() default {};
    }

    /**
     * @return Postgres image like: "postgres:15.3-alpine"
     */
    String image() default "postgres:15.3-alpine";

    /**
     * @return when to start container
     */
    StartMode startMode() default StartMode.PER_METHOD;

    Migration migration() default @Migration(engine = MigrationEngine.FLYWAY, apply = ExecuteMode.NONE, drop = ExecuteMode.NONE);
}
