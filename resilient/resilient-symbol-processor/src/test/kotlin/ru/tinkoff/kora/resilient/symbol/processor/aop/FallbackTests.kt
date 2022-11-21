package ru.tinkoff.kora.resilient.symbol.processor.aop

import com.google.devtools.ksp.KspExperimental
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.runBlocking
import org.junit.jupiter.api.Test
import org.junit.jupiter.api.TestInstance
import ru.tinkoff.kora.aop.symbol.processor.AopSymbolProcessorProvider
import ru.tinkoff.kora.ksp.common.CompilationErrorException
import ru.tinkoff.kora.ksp.common.symbolProcess
import ru.tinkoff.kora.resilient.symbol.processor.aop.testdata.FallbackIllegalArgumentTarget
import ru.tinkoff.kora.resilient.symbol.processor.aop.testdata.FallbackIllegalSignatureTarget
import ru.tinkoff.kora.resilient.symbol.processor.aop.testdata.FallbackTarget

@TestInstance(TestInstance.Lifecycle.PER_CLASS)
@KspExperimental
class FallbackTests : FallbackRunner() {

    private fun getService(graph: InitializedGraph): FallbackTarget {
        val values = graph.graphDraw.nodes
            .stream()
            .map { node -> graph.refreshableGraph.get(node) }
            .toList()

        return values.asSequence()
            .filter { a -> a is FallbackTarget }
            .map { a -> (a as FallbackTarget) }
            .first()
    }

    @Test
    fun incorrectArgumentFallback() {
        assertThrows(
            CompilationErrorException::class.java
        ) { symbolProcess(FallbackIllegalArgumentTarget::class, AopSymbolProcessorProvider()) }
    }

    @Test
    fun incorrectSignatureFallback() {
        assertThrows(
            CompilationErrorException::class.java
        ) { symbolProcess(FallbackIllegalSignatureTarget::class, AopSymbolProcessorProvider()) }
    }

    @Test
    fun voidFallback() {
        // given
        val graphDraw = createGraphDraw()
        val service = getService(graphDraw)
        assertEquals(FallbackTarget.VoidState.NONE, service.voidState)
        service.alwaysFail = false

        // when
        service.voidSync()
        assertEquals(FallbackTarget.VoidState.VALUE, service.voidState)
        service.alwaysFail = true

        // then
        service.voidSync()
        assertEquals(FallbackTarget.VoidState.FALLBACK, service.voidState)
    }

    @Test
    fun syncFallback() {
        // given
        val graphDraw = createGraphDraw()
        val service = getService(graphDraw)
        service.alwaysFail = false

        // when
        assertEquals(FallbackTarget.VALUE, service.getValueSync())
        service.alwaysFail = true

        // then
        assertEquals(FallbackTarget.FALLBACK, service.getValueSync())
    }

    @Test
    fun monoFallback() {
        // given
        val graphDraw = createGraphDraw()
        val service = getService(graphDraw)
        service.alwaysFail = false

        // when
        assertEquals(FallbackTarget.VALUE, runBlocking { service.getValueSuspend() })
        service.alwaysFail = true

        // then
        assertEquals(FallbackTarget.FALLBACK, runBlocking { service.getValueSuspend() })
    }

    @Test
    fun fluxFallback() {
        // given
        val graphDraw = createGraphDraw()
        val service = getService(graphDraw)
        service.alwaysFail = false

        // when
        assertEquals(FallbackTarget.VALUE, runBlocking { service.getValueFLow().first() })
        service.alwaysFail = true

        // then
        assertEquals(FallbackTarget.FALLBACK, runBlocking { service.getValueFLow().first() })
    }
}
