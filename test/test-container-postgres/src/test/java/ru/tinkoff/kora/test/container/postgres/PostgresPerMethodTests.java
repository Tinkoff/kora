package ru.tinkoff.kora.test.container.postgres;

import org.junit.jupiter.api.MethodOrderer;
import org.junit.jupiter.api.Order;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.TestMethodOrder;
import ru.tinkoff.kora.test.container.postgres.TestcontainersPostgres.StartMode;

import static org.junit.jupiter.api.Assertions.*;

@TestcontainersPostgres(startMode = StartMode.PER_METHOD, image = "postgres:15.1-alpine")
@TestMethodOrder(MethodOrderer.OrderAnnotation.class)
public class PostgresPerMethodTests {

    @ContainerPostgresConnection
    private PostgresConnection samePerMethodConnection;

    private static PostgresConnection firstConnection;

    @Order(1)
    @Test
    void firstConnection(@ContainerPostgresConnection PostgresConnection connection) {
        assertNull(firstConnection);
        assertNotNull(connection);
        assertNotNull(connection.jdbcUrl());
        firstConnection = connection;
        assertNotNull(samePerMethodConnection);
        assertEquals(samePerMethodConnection, connection);
    }

    @Order(2)
    @Test
    void secondConnection(@ContainerPostgresConnection PostgresConnection connection) {
        assertNotNull(connection);
        assertNotNull(connection.jdbcUrl());
        assertNotNull(samePerMethodConnection);
        assertEquals(samePerMethodConnection, connection);
        assertNotNull(firstConnection);
        assertNotEquals(firstConnection, connection);
    }
}
