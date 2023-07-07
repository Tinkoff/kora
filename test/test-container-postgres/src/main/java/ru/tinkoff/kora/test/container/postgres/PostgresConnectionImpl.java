package ru.tinkoff.kora.test.container.postgres;

import org.intellij.lang.annotations.Language;
import org.testcontainers.containers.PostgreSQLContainer;

import javax.annotation.Nonnull;
import java.sql.Connection;
import java.sql.DriverManager;
import java.sql.SQLException;

record PostgresConnectionImpl(String host, int port, String database, String username, String password) implements PostgresConnection {

    PostgresConnectionImpl(PostgreSQLContainer<?> container) {
        this(container.getHost(),
            container.getMappedPort(PostgreSQLContainer.POSTGRESQL_PORT),
            container.getDatabaseName(),
            container.getUsername(),
            container.getPassword());
    }

    @Nonnull
    @Override
    public String jdbcUrl() {
        return "jdbc:postgresql://%s:%d/%s".formatted(host, port, database);
    }

    @Nonnull
    @Override
    public String r2dbcUrl() {
        return "r2dbc:postgresql://%s:%d/%s".formatted(host, port, database);
    }

    @Nonnull
    @Override
    public Connection open() {
        try {
            return DriverManager.getConnection(jdbcUrl(), username, password);
        } catch (SQLException e) {
            throw new RuntimeException(e);
        }
    }

    @Override
    public void execute(@Language("SQL") String sql) {
        try (var connection = open();
             var stmt = connection.createStatement()) {
            stmt.execute(sql);
        } catch (SQLException e) {
            throw new IllegalStateException(e);
        }
    }

    @Override
    public <T, E extends Throwable> T execute(@Language("SQL") String sql, @Nonnull ResultSetMapper<T, E> extractor) throws E {
        try (var connection = open();
             var stmt = connection.prepareStatement(sql);
             var rs = stmt.executeQuery()) {
            return extractor.apply(rs);
        } catch (SQLException e) {
            throw new IllegalStateException(e);
        }
    }
}
