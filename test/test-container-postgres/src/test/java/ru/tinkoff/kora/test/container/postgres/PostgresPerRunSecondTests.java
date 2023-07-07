package ru.tinkoff.kora.test.container.postgres;

import org.junit.jupiter.api.MethodOrderer;
import org.junit.jupiter.api.Order;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.TestMethodOrder;
import ru.tinkoff.kora.test.container.postgres.TestcontainersPostgres.StartMode;

import static org.junit.jupiter.api.Assertions.*;

@TestcontainersPostgres(startMode = StartMode.PER_RUN, image = "postgres:15.3-alpine")
@TestMethodOrder(MethodOrderer.OrderAnnotation.class)
public class PostgresPerRunSecondTests {

    static volatile PostgresConnection perRunConnection;

    @ContainerPostgresConnection
    private PostgresConnection sameConnection;

    @Order(1)
    @Test
    void firstConnection(@ContainerPostgresConnection PostgresConnection connection) {
        assertNotNull(connection);
        assertNotNull(connection.jdbcUrl());
        assertNotNull(sameConnection);
        assertEquals(sameConnection, connection);

        if(perRunConnection == null) {
            perRunConnection = connection;
        }

        if(PostgresPerRunFirstTests.perRunConnection != null) {
            assertEquals(perRunConnection, PostgresPerRunFirstTests.perRunConnection);
        }
    }

    @Order(2)
    @Test
    void secondConnection(@ContainerPostgresConnection PostgresConnection connection) {
        assertNotNull(connection);
        assertNotNull(connection.jdbcUrl());
        assertNotNull(sameConnection);
        assertEquals(sameConnection, connection);
        assertEquals(perRunConnection, connection);
    }
}
