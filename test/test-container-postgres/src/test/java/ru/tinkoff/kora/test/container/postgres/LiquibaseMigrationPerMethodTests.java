package ru.tinkoff.kora.test.container.postgres;

import org.junit.jupiter.api.MethodOrderer;
import org.junit.jupiter.api.Order;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.TestMethodOrder;
import ru.tinkoff.kora.test.container.postgres.TestcontainersPostgres.ExecuteMode;
import ru.tinkoff.kora.test.container.postgres.TestcontainersPostgres.Migration;
import ru.tinkoff.kora.test.container.postgres.TestcontainersPostgres.MigrationEngine;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;

@TestcontainersPostgres(startMode = TestcontainersPostgres.StartMode.PER_CLASS,
    image = "postgres:15.1-alpine",
    migration = @Migration(
        engine = MigrationEngine.LIQUIBASE,
        apply = ExecuteMode.PER_METHOD,
        drop = ExecuteMode.PER_METHOD
    ))
@TestMethodOrder(MethodOrderer.OrderAnnotation.class)
public class LiquibaseMigrationPerMethodTests {

    @Order(1)
    @Test
    void firstRun(@ContainerPostgresConnection PostgresConnection connection) {
        connection.execute("INSERT INTO users VALUES(1);");
    }

    @Order(2)
    @Test
    void secondRun(@ContainerPostgresConnection PostgresConnection connection) {
        var usersFound = connection.execute("SELECT * FROM users;", r -> (!r.next())
            ? -1
            : r.getInt(1));
        assertEquals(-1, usersFound);
    }
}
