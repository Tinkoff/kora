package ru.tinkoff.kora.cache.symbol.processor

import com.google.devtools.ksp.KspExperimental
import org.junit.jupiter.api.Assertions
import org.junit.jupiter.api.Test
import ru.tinkoff.kora.cache.symbol.processor.testdata.*
import ru.tinkoff.kora.cache.symbol.processor.testdata.reactive.flux.CacheableTargetGetFlux
import ru.tinkoff.kora.cache.symbol.processor.testdata.reactive.flux.CacheableTargetPutFlux
import ru.tinkoff.kora.cache.symbol.processor.testdata.reactive.mono.CacheableTargetGetMono
import ru.tinkoff.kora.cache.symbol.processor.testdata.reactive.mono.CacheableTargetPutMono
import ru.tinkoff.kora.cache.symbol.processor.testdata.reactive.publisher.CacheableTargetGetPublisher
import ru.tinkoff.kora.cache.symbol.processor.testdata.reactive.publisher.CacheableTargetPutPublisher
import ru.tinkoff.kora.cache.symbol.processor.testdata.suspended.CacheableTargetSuspend
import ru.tinkoff.kora.ksp.common.CompilationErrorException
import ru.tinkoff.kora.ksp.common.symbolProcess

@KspExperimental
class CacheSymbolProcessorTests : Assertions() {

    @Test
    @Throws(Exception::class)
    fun cacheKeyRecordGeneratedForSync() {
        val classLoader = symbolProcess(
            CacheableTargetSync::class,
            CacheSymbolProcessorProvider()
        )
        val clazz = classLoader.loadClass("ru.tinkoff.kora.cache.symbol.processor.testdata._CacheKey__sync_cache")
        assertNotNull(clazz)
    }

    @Test
    @Throws(Exception::class)
    fun cacheKeyRecordGeneratedForSuspend() {
        val classLoader = symbolProcess(
            CacheableTargetSuspend::class,
            CacheSymbolProcessorProvider()
        )
        val clazz = classLoader.loadClass("ru.tinkoff.kora.cache.symbol.processor.testdata.suspended._CacheKey__suspend_cache")
        assertNotNull(clazz)
    }

    @Test
    fun cacheKeyArgumentMissing() {
        assertThrows(
            CompilationErrorException::class.java
        ) {
            symbolProcess(
                CacheableTargetArgumentMissing::class,
                CacheSymbolProcessorProvider()
            )
        }
    }

    @Test
    fun cacheKeyArgumentWrongOrder() {
        assertThrows(
            CompilationErrorException::class.java
        ) {
            symbolProcess(
                CacheableTargetArgumentWrongOrder::class,
                CacheSymbolProcessorProvider()
            )
        }
    }

    @Test
    fun cacheKeyArgumentWrongType() {
        assertThrows(
            CompilationErrorException::class.java
        ) {
            symbolProcess(
                CacheableTargetArgumentWrongType::class,
                CacheSymbolProcessorProvider()
            )
        }
    }

    @Test
    fun cacheNamePatternMismatch() {
        assertThrows(
            CompilationErrorException::class.java
        ) { symbolProcess(CacheableTargetNameInvalid::class, CacheSymbolProcessorProvider()) }
    }

    @Test
    fun cacheGetForVoidSignature() {
        assertThrows(
            CompilationErrorException::class.java
        ) { symbolProcess(CacheableTargetGetVoid::class, CacheSymbolProcessorProvider()) }
    }

    @Test
    fun cachePutForVoidSignature() {
        assertThrows(
            CompilationErrorException::class.java
        ) { symbolProcess(CacheableTargetPutVoid::class, CacheSymbolProcessorProvider()) }
    }

    @Test
    fun cacheGetForMonoSignature() {
        assertThrows(
            CompilationErrorException::class.java
        ) { symbolProcess(CacheableTargetGetMono::class, CacheSymbolProcessorProvider()) }
    }

    @Test
    fun cachePutForMonoSignature() {
        assertThrows(
            CompilationErrorException::class.java
        ) { symbolProcess(CacheableTargetPutMono::class, CacheSymbolProcessorProvider()) }
    }

    @Test
    fun cacheGetForFluxSignature() {
        assertThrows(
            CompilationErrorException::class.java
        ) { symbolProcess(CacheableTargetGetFlux::class, CacheSymbolProcessorProvider()) }
    }

    @Test
    fun cachePutForFluxSignature() {
        assertThrows(
            CompilationErrorException::class.java
        ) { symbolProcess(CacheableTargetPutFlux::class, CacheSymbolProcessorProvider()) }
    }

    @Test
    fun cacheGetForPublisherSignature() {
        assertThrows(
            CompilationErrorException::class.java
        ) { symbolProcess(CacheableTargetGetPublisher::class, CacheSymbolProcessorProvider()) }
    }

    @Test
    fun cachePutForPublisherSignature() {
        assertThrows(
            CompilationErrorException::class.java
        ) { symbolProcess(CacheableTargetPutPublisher::class, CacheSymbolProcessorProvider()) }
    }
}
