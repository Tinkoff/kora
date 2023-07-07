package ru.tinkoff.kora.test.container.postgres;

import org.intellij.lang.annotations.Language;

import javax.annotation.Nonnull;
import java.sql.Connection;
import java.sql.ResultSet;
import java.sql.SQLException;

public interface PostgresConnection {

    interface ResultSetMapper<R, E extends Throwable> {
        R apply(ResultSet rs) throws SQLException, E;
    }

    @Nonnull
    String host();

    int port();

    @Nonnull
    String database();

    @Nonnull
    String username();

    @Nonnull
    String password();

    @Nonnull
    String jdbcUrl();

    @Nonnull
    String r2dbcUrl();

    @Nonnull
    Connection open();

    void execute(@Language("SQL") String sql);

    <T, E extends Throwable> T execute(@Language("SQL") String sql, @Nonnull ResultSetMapper<T, E> extractor) throws E;
}
