package ru.tinkoff.kora.database.vertx.coroutines

import io.vertx.sqlclient.Pool
import io.vertx.sqlclient.SqlConnection
import kotlinx.coroutines.ExperimentalCoroutinesApi
import ru.tinkoff.kora.database.common.telemetry.DataBaseTelemetry

@ExperimentalCoroutinesApi
interface VertxConnectionFactory {

    suspend fun currentConnection(): SqlConnection?

    suspend fun newConnection(): SqlConnection

    fun pool(): Pool

    fun telemetry(): DataBaseTelemetry

    suspend fun <T: Any?> withConnection(callback: suspend (SqlConnection) -> T): T

    suspend fun <T: Any?> inTx(callback: suspend (SqlConnection) -> T): T
}
