package ru.tinkoff.kora.resilient.symbol.processor.aop

import com.google.devtools.ksp.KspExperimental
import kotlinx.coroutines.TimeoutCancellationException
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.runBlocking
import org.junit.jupiter.api.Test
import org.junit.jupiter.api.TestInstance
import ru.tinkoff.kora.resilient.symbol.processor.aop.testdata.TimeoutTarget
import ru.tinkoff.kora.resilient.timeout.TimeoutException

@TestInstance(TestInstance.Lifecycle.PER_CLASS)
@KspExperimental
class TimeoutTests : TimeoutRunner() {

    private fun getService(graph: InitializedGraph): TimeoutTarget {
        val values = graph.graphDraw.nodes
            .stream()
            .map { node -> graph.refreshableGraph.get(node) }
            .toList()

        return values.asSequence()
            .filter { a -> a is TimeoutTarget }
            .map { a -> a as TimeoutTarget }
            .first()
    }

    @Test
    fun syncTimeout() {
        // given
        val graphDraw = createGraphDraw()
        val service = getService(graphDraw)
        assertThrows(TimeoutException::class.java) { service.getValueSync() }
    }

    @Test
    fun suspendTimeout() {
        // given
        val graphDraw = createGraphDraw()
        val service = getService(graphDraw)

        // then
        assertThrows(TimeoutCancellationException::class.java) { runBlocking { service.getValueSuspend() } }
    }

    @Test
    fun flowTimeout() {
        // given
        val graphDraw = createGraphDraw()
        val service = getService(graphDraw)

        // when
        assertThrows(TimeoutException::class.java) { runBlocking { service.getValueFLow().first() } }
    }
}
