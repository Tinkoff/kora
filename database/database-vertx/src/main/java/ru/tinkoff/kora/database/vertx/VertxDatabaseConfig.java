package ru.tinkoff.kora.database.vertx;

import io.vertx.pgclient.PgConnectOptions;
import io.vertx.sqlclient.PoolOptions;

import javax.annotation.Nullable;
import java.time.Duration;
import java.util.concurrent.TimeUnit;

public record VertxDatabaseConfig(
    String host,
    int port,
    String database,
    String username,
    String password,
    String poolName,
    Duration connectionTimeout,
    Duration idleTimeout,
    Duration acquireTimeout,
    int maxPoolSize) {

    public VertxDatabaseConfig(
        String host,
        int port,
        String database,
        String username,
        String password,
        String poolName,
        @Nullable Duration connectionTimeout,
        @Nullable Duration idleTimeout,
        @Nullable Duration acquireTimeout,
        @Nullable Integer maxPoolSize) {
        this(
            host,
            port,
            database,
            username,
            password,
            poolName,
            defaultConnectionTimeout(connectionTimeout),
            idleTimeout != null ? idleTimeout : Duration.ofMinutes(10),
            acquireTimeout != null ? acquireTimeout : defaultConnectionTimeout(connectionTimeout),
            maxPoolSize != null ? maxPoolSize : 10
        );
    }

    private static Duration defaultConnectionTimeout(Duration connectionTimeout) {
        return connectionTimeout != null ? connectionTimeout : Duration.ofSeconds(30);
    }

    public PgConnectOptions toPgConnectOptions() {
        return new PgConnectOptions()
            .setMetricsName(this.poolName)
            .setHost(this.host)
            .setPort(this.port)
            .setDatabase(this.database)
            .setUser(this.username)
            .setPassword(this.password)
            .setConnectTimeout(((Long) this.connectionTimeout.toMillis()).intValue())
            .setIdleTimeout(((Long) this.idleTimeout.toMillis()).intValue())
            .setIdleTimeoutUnit(TimeUnit.MILLISECONDS)
            .setCachePreparedStatements(true);
    }

    public PoolOptions toPgPoolOptions() {
        return new PoolOptions()
            .setConnectionTimeout(((Long) this.connectionTimeout.toMillis()).intValue())
            .setConnectionTimeoutUnit(TimeUnit.MILLISECONDS)
            .setIdleTimeout(((Long) this.idleTimeout.toMillis()).intValue())
            .setIdleTimeoutUnit(TimeUnit.MILLISECONDS)
            .setName(this.poolName)
            .setMaxSize(this.maxPoolSize);
    }
}
