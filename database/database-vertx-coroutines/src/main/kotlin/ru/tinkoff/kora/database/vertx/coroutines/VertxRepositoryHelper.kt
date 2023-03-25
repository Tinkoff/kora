package ru.tinkoff.kora.database.vertx.coroutines

import io.vertx.core.AsyncResult
import io.vertx.kotlin.coroutines.await
import io.vertx.sqlclient.PreparedStatement
import io.vertx.sqlclient.SqlClient
import io.vertx.sqlclient.SqlConnection
import io.vertx.sqlclient.Tuple
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.future.await
import kotlinx.coroutines.reactive.asFlow
import reactor.core.publisher.Flux
import ru.tinkoff.kora.common.Context
import ru.tinkoff.kora.database.common.QueryContext
import ru.tinkoff.kora.database.common.UpdateCount
import ru.tinkoff.kora.database.common.telemetry.DataBaseTelemetry
import ru.tinkoff.kora.database.vertx.mapper.result.VertxRowMapper
import ru.tinkoff.kora.database.vertx.mapper.result.VertxRowSetMapper
import java.util.concurrent.CompletableFuture
import kotlin.coroutines.coroutineContext

@ExperimentalCoroutinesApi
@Suppress("MemberVisibilityCanBePrivate")
object VertxRepositoryHelper {

    suspend fun await(connectionFactory: VertxConnectionFactory, query: QueryContext, params: Tuple?) {
        awaitSingleOrNull(connectionFactory, query, params) {}
    }

    suspend fun await(connection: SqlClient, t: DataBaseTelemetry, query: QueryContext, params: Tuple?) {
        awaitSingleOrNull(connection, t, query, params) {}
    }

    suspend fun <T> awaitSingleOrNull(connectionFactory: VertxConnectionFactory, query: QueryContext, params: Tuple?, mapper: VertxRowSetMapper<T>): T? {
        val currentConnection: SqlConnection? = connectionFactory.currentConnection()
        if (currentConnection != null) {
            return awaitSingleOrNull(currentConnection, connectionFactory.telemetry(), query, params, mapper)
        }
        var connection: SqlConnection? = null
        try {
            connection = connectionFactory.newConnection()
            return awaitSingleOrNull(connection, connectionFactory.telemetry(), query, params, mapper)
        } finally {
            connection?.close()
                ?.await()
        }
    }

    suspend fun <T> awaitSingleOrNull(connection: SqlClient, t: DataBaseTelemetry, query: QueryContext, params: Tuple?, mapper: VertxRowSetMapper<T>): T? {
        val currentCoroutineContext = coroutineContext
        val ctx = Context.Kotlin.current(currentCoroutineContext)
        val telemetry = t.createContext(ctx, query)
        val future = CompletableFuture<T?>()
        connection.preparedQuery(query.sql)
            .execute(params) { rowSetEvent ->
                Context.Kotlin.inject(currentCoroutineContext, ctx)
                if (rowSetEvent.failed()) {
                    telemetry.close(rowSetEvent.cause())
                    future.completeExceptionally(rowSetEvent.cause())
                } else {
                    val result: T = try {
                        val rowSet = rowSetEvent.result()
                        mapper.apply(rowSet)
                    } catch (e: Exception) {
                        telemetry.close(e)
                        future.completeExceptionally(e)
                        return@execute
                    }
                    telemetry.close(null)
                    future.complete(result)
                }
            }
        return future.await()
    }

    suspend fun awaitBatch(connectionFactory: VertxConnectionFactory, query: QueryContext, params: List<Tuple?>?): UpdateCount {
        val currentConnection: SqlConnection? = connectionFactory.currentConnection()
        if (currentConnection != null) {
            return awaitBatch(currentConnection, connectionFactory.telemetry(), query, params)
        }
        var connection: SqlConnection? = null
        try {
            connection = connectionFactory.newConnection()
            return awaitBatch(connection, connectionFactory.telemetry(), query, params)
        } finally {
            connection?.close()
                ?.await()
        }
    }

    suspend fun awaitBatch(connection: SqlClient, t: DataBaseTelemetry, query: QueryContext, params: List<Tuple?>?): UpdateCount {
        val currentCoroutineContext = coroutineContext
        val ctx = Context.Kotlin.current(currentCoroutineContext)
        val telemetry = t.createContext(ctx, query)
        val future = CompletableFuture<UpdateCount>()
        connection.preparedQuery(query.sql).executeBatch(params) { rowSetEvent ->
            Context.Kotlin.inject(currentCoroutineContext, ctx)
            if (rowSetEvent.failed()) {
                telemetry.close(rowSetEvent.cause())
                future.completeExceptionally(rowSetEvent.cause())
            } else {
                var result = 0
                try {
                    var rowSet = rowSetEvent.result()
                    while (rowSet != null) {
                        result += rowSet.rowCount()
                        rowSet = rowSet.next()
                    }
                } catch (e: Exception) {
                    telemetry.close(e)
                    future.completeExceptionally(e)
                    return@executeBatch
                }
                telemetry.close(null)
                future.complete(UpdateCount(result.toLong()))
            }
        }
        return future.await()
    }

    suspend fun <T> flow(connectionFactory: VertxConnectionFactory, query: QueryContext, params: Tuple?, mapper: VertxRowMapper<T>): Flow<T> {
        val currentConnection: SqlConnection? = connectionFactory.currentConnection()
        if (currentConnection != null) {
            return flow(currentConnection, connectionFactory.telemetry(), query, params, mapper)
        }

        var connection: SqlConnection? = null
        try {
            connection = connectionFactory.newConnection()
            return flow(connection, connectionFactory.telemetry(), query, params, mapper)
        } finally {
            connection?.close()
                ?.await()
        }
    }

    suspend fun <T> flow(connection: SqlConnection, telemetry: DataBaseTelemetry, query: QueryContext, params: Tuple?, mapper: VertxRowMapper<T>): Flow<T> {
        val currentCoroutineContext = coroutineContext
        val ctx = Context.Kotlin.current(currentCoroutineContext)

        return Flux.create { sink ->
            Context.Kotlin.inject(currentCoroutineContext, ctx)
            val tctx = telemetry.createContext(ctx, query)
            connection.prepare(query.sql) { statementEvent: AsyncResult<PreparedStatement> ->
                if (statementEvent.failed()) {
                    tctx.close(statementEvent.cause())
                    sink.error(statementEvent.cause())
                    return@prepare
                }
                val stmt = statementEvent.result()
                val stream = stmt.createStream(50, params).pause()
                sink.onDispose { stream.close() }
                sink.onRequest { stream.fetch(it) }
                stream.exceptionHandler { throwable ->
                    stmt.close()
                    tctx.close(null)
                    sink.error(throwable)
                }
                stream.endHandler {
                    stmt.close()
                    tctx.close(null)
                    sink.complete()
                }
                stream.handler { row ->
                    val mappedRow = mapper.apply(row)
                    sink.next(mappedRow)
                }
            }
        }.asFlow()
    }
}
