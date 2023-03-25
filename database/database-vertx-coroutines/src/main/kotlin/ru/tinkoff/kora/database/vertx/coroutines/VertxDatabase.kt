package ru.tinkoff.kora.database.vertx.coroutines

import io.netty.channel.EventLoopGroup
import io.vertx.kotlin.coroutines.await
import io.vertx.pgclient.PgPool
import io.vertx.sqlclient.Pool
import io.vertx.sqlclient.SqlConnection
import io.vertx.sqlclient.Transaction
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.reactor.mono
import reactor.core.publisher.Mono
import ru.tinkoff.kora.application.graph.Lifecycle
import ru.tinkoff.kora.application.graph.Wrapped
import ru.tinkoff.kora.common.Context
import ru.tinkoff.kora.database.common.telemetry.DataBaseTelemetry
import ru.tinkoff.kora.database.common.telemetry.DataBaseTelemetryFactory
import ru.tinkoff.kora.database.vertx.VertxDatabaseConfig
import ru.tinkoff.kora.vertx.common.VertxUtil
import kotlin.coroutines.coroutineContext

@ExperimentalCoroutinesApi
class VertxDatabase(
    vertxDatabaseConfig: VertxDatabaseConfig,
    eventLoopGroup: EventLoopGroup,
    telemetryFactory: DataBaseTelemetryFactory
) : Lifecycle, Wrapped<Pool>, VertxConnectionFactory {

    private val connectionKey: Context.Key<SqlConnection> = object : Context.Key<SqlConnection>() {
        override fun copy(connection: SqlConnection): SqlConnection? {
            return null
        }
    }
    private val transactionKey: Context.Key<Transaction> = object : Context.Key<Transaction>() {
        override fun copy(transaction: Transaction): Transaction? {
            return null
        }
    }
    private val pool: Pool
    private val telemetry: DataBaseTelemetry

    init {
        telemetry = telemetryFactory[vertxDatabaseConfig.poolName, "postgres", vertxDatabaseConfig.username]
        pool = PgPool.pool(VertxUtil.customEventLoopVertx(eventLoopGroup), vertxDatabaseConfig.toPgConnectOptions(), vertxDatabaseConfig.toPgPoolOptions())
    }

    override suspend fun currentConnection(): SqlConnection? {
        val ctx = Context.Kotlin.current(coroutineContext)
        return ctx.get(this.connectionKey)
    }

    override suspend fun newConnection(): SqlConnection {
        return this.pool.connection
            .await()
    }

    override fun pool(): Pool = this.pool

    override fun telemetry(): DataBaseTelemetry = this.telemetry

    override suspend fun <T> withConnection(callback: suspend (SqlConnection) -> T): T {
        val ctx = Context.Kotlin.current(coroutineContext)
        val currentConnection = ctx[connectionKey]
        if (currentConnection != null) {
            return callback(currentConnection)
        }
        var connection: SqlConnection? = null
        return try {
            connection = newConnection()
            ctx[connectionKey] = connection
            Context.Kotlin.inject(coroutineContext, ctx)
            callback(connection)
        } finally {
            connection?.close()
                ?.await()
            ctx.remove(connectionKey)
        }
    }

    override suspend fun <T> inTx(callback: suspend (SqlConnection) -> T): T {
        return withConnection { connection ->
            val ctx = Context.Kotlin.current(coroutineContext)
            val currentTransaction = ctx[this.transactionKey]
            if (currentTransaction != null) {
                callback(connection)
            } else {
                val tx = connection.begin()
                    .await()
                try {
                    ctx[this.transactionKey] = tx
                    val result = try {
                        callback(connection)
                    } catch (ex: Exception) {
                        tx.rollback()
                            .await()
                        throw ex
                    }
                    tx.commit()
                        .await()
                    result
                } finally {
                    ctx.remove(this.transactionKey)
                }
            }
        }
    }

    override fun init(): Mono<*> = mono {
        pool.query("SELECT 1")
            .execute()
            .await()
    }

    override fun release(): Mono<*> = mono {
        pool.close()
            .await()
    }

    override fun value(): Pool = this.pool
}
