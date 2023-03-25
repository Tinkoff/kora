package ru.tinkoff.kora.database.vertx.coroutines

import io.netty.channel.nio.NioEventLoopGroup
import io.vertx.sqlclient.Tuple
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.runBlocking
import org.assertj.core.api.Assertions
import org.junit.jupiter.api.AfterAll
import org.junit.jupiter.api.BeforeAll
import org.junit.jupiter.api.Test
import org.junit.jupiter.api.assertThrows
import org.junit.jupiter.api.extension.ExtendWith
import ru.tinkoff.kora.database.common.QueryContext
import ru.tinkoff.kora.database.common.telemetry.DefaultDataBaseTelemetryFactory
import ru.tinkoff.kora.database.vertx.VertxDatabaseConfig
import ru.tinkoff.kora.database.vertx.mapper.result.VertxRowSetMapper
import ru.tinkoff.kora.test.postgres.PostgresParams
import ru.tinkoff.kora.test.postgres.PostgresParams.ResultSetMapper
import ru.tinkoff.kora.test.postgres.PostgresTestContainer
import ru.tinkoff.kora.vertx.common.VertxUtil
import java.sql.SQLException
import java.time.Duration
import java.util.function.Consumer

@OptIn(ExperimentalCoroutinesApi::class)
@ExtendWith(PostgresTestContainer::class)
class VertxConnectionFactoryTest {

    @Test
    fun testQuery(params: PostgresParams) {
        params.execute(
            """
            CREATE TABLE test_table(id BIGSERIAL, value VARCHAR);
            INSERT INTO test_table(value) VALUES ('test1');
            INSERT INTO test_table(value) VALUES ('test2');
            
            """
                .trimIndent()
        )
        val id = "SELECT * FROM test_table WHERE value = :value"
        val sql = "SELECT * FROM test_table WHERE value = $1"

        data class Entity(val id: Long, val value: String)

        val mapper = VertxRowSetMapper { rows ->
            Assertions.assertThat(rows.size() == 1)
            val row = rows.iterator().next()
            Entity(row.getLong(0), row.getString(1))
        }

        withDb(params) { db ->
            val entity = runBlocking {
                db.withConnection { connection ->
                    VertxRepositoryHelper.awaitSingleOrNull(connection, db.telemetry(), QueryContext(id, sql), Tuple.of("test1"), mapper)
                }
            }
            Assertions.assertThat(entity)
                .isNotNull
                .isEqualTo(Entity(1, "test1"))
        }
    }

    @Test
    fun testTransaction(params: PostgresParams) {
        params.execute("CREATE TABLE test_table(id BIGSERIAL, value VARCHAR);")
        val id = "INSERT INTO test_table(value) VALUES ('test1');"
        val sql = "INSERT INTO test_table(value) VALUES ('test1');"
        val extractor = ResultSetMapper<List<String>, RuntimeException> { rs ->
            val result = ArrayList<String>()
            try {
                while (rs.next()) {
                    result.add(rs.getString(1))
                }
            } catch (sqlException: SQLException) {
                throw RuntimeException(sqlException)
            }
            result
        }

        withDb(params) { db ->
            assertThrows<RuntimeException> {
                runBlocking {
                    db.inTx { connection ->
                        VertxRepositoryHelper.await(connection, db.telemetry(), QueryContext(id, sql), Tuple.tuple())
                        throw RuntimeException("test")
                    }
                }
            }
            var values = params.query("SELECT value FROM test_table", extractor)
            Assertions.assertThat(values).hasSize(0)

            runBlocking {
                db.inTx { connection ->
                    VertxRepositoryHelper.await(connection, db.telemetry(), QueryContext(id, sql), Tuple.tuple())
                }
            }
            values = params.query("SELECT value FROM test_table", extractor)
            Assertions.assertThat(values).hasSize(1)
        }
    }

    companion object {

        private var eventLoopGroup: NioEventLoopGroup? = null

        @BeforeAll
        @JvmStatic
        fun beforeAll() {
            eventLoopGroup = NioEventLoopGroup(1, VertxUtil.vertxThreadFactory())
        }

        @AfterAll
        @JvmStatic
        fun afterAll() {
            eventLoopGroup!!.shutdownGracefully()
        }

        private fun withDb(params: PostgresParams, consumer: Consumer<VertxDatabase>) {
            val config = VertxDatabaseConfig(
                params.user,
                params.password,
                params.host,
                params.port,
                params.db,
                "test",
                Duration.ofMillis(1000),
                Duration.ofMillis(1000),
                Duration.ofMillis(1000),
                1,
                true
            )
            val db = VertxDatabase(config, eventLoopGroup!!, DefaultDataBaseTelemetryFactory(null, null, null))
            db.init().block()
            try {
                consumer.accept(db)
            } finally {
                db.release().block()
            }
        }
    }
}
