package ru.tinkoff.kora.test.container.postgres;

import org.junit.jupiter.api.MethodOrderer;
import org.junit.jupiter.api.Order;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.TestMethodOrder;
import ru.tinkoff.kora.test.container.postgres.TestcontainersPostgres.StartMode;

import static org.junit.jupiter.api.Assertions.*;

@TestcontainersPostgres(startMode = StartMode.PER_CLASS, image = "postgres:15.2-alpine")
@TestMethodOrder(MethodOrderer.OrderAnnotation.class)
public class PostgresPerClassTests {

    @ContainerPostgresConnection
    private PostgresConnection sameConnection;

    private static PostgresConnection firstConnection;

    @Order(1)
    @Test
    void firstConnection(@ContainerPostgresConnection PostgresConnection connection) {
        assertNull(firstConnection);
        assertNotNull(connection);
        assertNotNull(connection.jdbcUrl());
        assertNotNull(sameConnection);
        assertEquals(sameConnection, connection);
        firstConnection = connection;
    }

    @Order(2)
    @Test
    void secondConnection(@ContainerPostgresConnection PostgresConnection connection) {
        assertNotNull(connection);
        assertNotNull(connection.jdbcUrl());
        assertNotNull(firstConnection);
        assertNotNull(sameConnection);
        assertEquals(sameConnection, connection);
        assertEquals(firstConnection, connection);
    }
}
