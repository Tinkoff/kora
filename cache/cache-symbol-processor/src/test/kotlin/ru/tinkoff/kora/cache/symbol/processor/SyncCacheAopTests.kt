package ru.tinkoff.kora.cache.symbol.processor

import com.google.devtools.ksp.KspExperimental
import org.junit.jupiter.api.Assertions.*
import org.junit.jupiter.api.BeforeEach
import org.junit.jupiter.api.Test
import org.junit.jupiter.api.TestInstance
import ru.tinkoff.kora.aop.symbol.processor.AopSymbolProcessorProvider
import ru.tinkoff.kora.cache.CacheKey
import ru.tinkoff.kora.cache.caffeine.CaffeineCacheConfig
import ru.tinkoff.kora.cache.caffeine.CaffeineCacheModule
import ru.tinkoff.kora.cache.symbol.processor.testcache.DummyCache2
import ru.tinkoff.kora.cache.symbol.processor.testdata.CacheableSync
import ru.tinkoff.kora.ksp.common.symbolProcess
import java.math.BigDecimal

@TestInstance(TestInstance.Lifecycle.PER_CLASS)
@KspExperimental
class SyncCacheAopTests : CaffeineCacheModule {

    private val CACHE_CLASS = "ru.tinkoff.kora.cache.symbol.processor.testcache.\$DummyCache2Impl"
    private val SERVICE_CLASS = "ru.tinkoff.kora.cache.symbol.processor.testdata.\$CacheableSync__AopProxy"

    private var cache: DummyCache2? = null
    private var cachedService: CacheableSync? = null

    private fun getService(): CacheableSync {
        if (cachedService != null) {
            return cachedService as CacheableSync;
        }

        return try {
            val classLoader = symbolProcess(
                listOf(DummyCache2::class, CacheableSync::class),
                CacheSymbolProcessorProvider(),
                AopSymbolProcessorProvider(),
            )

            val cacheClass = classLoader.loadClass(CACHE_CLASS) ?: throw IllegalArgumentException("Expected class not found: $CACHE_CLASS")
            cache = cacheClass.constructors[0].newInstance(
                CaffeineCacheConfig(null, null, null, null),
                caffeineCacheFactory(),
                defaultCacheTelemetry(null, null)
            ) as DummyCache2

            val serviceClass = classLoader.loadClass(SERVICE_CLASS) ?: throw IllegalArgumentException("Expected class not found: $SERVICE_CLASS")
            val inst = serviceClass.constructors[0].newInstance(cache) as CacheableSync
            inst
        } catch (e: Exception) {
            throw IllegalStateException(e.message, e)
        }
    }

    @BeforeEach
    fun reset() {
        cache?.invalidateAll()
    }

    @Test
    fun getWhenWasCacheEmpty() {
        // given
        val service = getService()
        service.value = "1"
        assertNotNull(service)

        // when
        val notCached = service.getValue("1", BigDecimal.ZERO)
        service.value = "2"

        // then
        val fromCache = service.getValue("1", BigDecimal.ZERO)
        assertEquals(notCached, fromCache)
        assertNotEquals("2", fromCache)
    }

    @Test
    fun getWhenCacheFilled() {
        // given
        val service = getService()
        service.value = "1"
        assertNotNull(service)

        // when
        val initial = service.getValue("1", BigDecimal.ZERO)
        val cached = service.putValue(BigDecimal.ZERO, "5", "1")
        assertEquals(initial, cached)
        service.value = "2"

        // then
        val fromCache = service.getValue("1", BigDecimal.ZERO)
        assertEquals(cached, fromCache)
    }

    @Test
    fun getWrongKeyWhenCacheFilled() {
        // given
        val service = getService()
        service.value = "1"
        assertNotNull(service)

        // when
        val initial = service.getValue("1", BigDecimal.ZERO)
        val cached = service.putValue(BigDecimal.ZERO, "5", "1")
        assertEquals(initial, cached)
        service.value = "2"

        // then
        val fromCache = service.getValue("2", BigDecimal.ZERO)
        assertNotEquals(cached, fromCache)
        assertEquals(service.value, fromCache)
    }

    @Test
    fun getWhenCacheFilledOtherKey() {
        // given
        val service = getService()
        service.value = "1"
        assertNotNull(service)

        // when
        val cached = service.putValue(BigDecimal.ZERO, "5", "1")
        service.value = "2"
        val initial = service.getValue("2", BigDecimal.ZERO)
        assertNotEquals(cached, initial)

        // then
        val fromCache = service.getValue("2", BigDecimal.ZERO)
        assertNotEquals(cached, fromCache)
        assertEquals(initial, fromCache)
    }

    @Test
    fun getWhenCacheInvalidate() {
        // given
        val service = getService()
        service.value = "1"
        assertNotNull(service)

        // when
        val initial = service.getValue("1", BigDecimal.ZERO)
        val cached = service.putValue(BigDecimal.ZERO, "5", "1")
        assertEquals(initial, cached)

        val cached2 = service.putValue(BigDecimal.ZERO, "5", "2")
        assertEquals(initial, cached2)

        service.value = "2"
        service.evictValue("1", BigDecimal.ZERO)

        // then
        assertNull(cache!!.get(CacheKey.of("1", BigDecimal.ZERO)))
        assertEquals(cached2, cache!!.get(CacheKey.of("2", BigDecimal.ZERO)))

        val fromCache = service.getValue("1", BigDecimal.ZERO)
        assertNotEquals(cached, fromCache)
    }

    @Test
    fun getWhenCacheInvalidateAll() {
        // given
        val service = getService()
        service.value = "1"
        assertNotNull(service)

        // when
        val initial = service.getValue("1", BigDecimal.ZERO)
        val cached = service.putValue(BigDecimal.ZERO, "5", "1")
        assertEquals(initial, cached)

        val cached2 = service.putValue(BigDecimal.ZERO, "5", "2")
        assertEquals(initial, cached2)

        service.value = "2"
        service.evictAll()

        // then
        assertNull(cache!!.get(CacheKey.of("1", BigDecimal.ZERO)))
        assertNull(cache!!.get(CacheKey.of("2", BigDecimal.ZERO)))

        val fromCache = service.getValue("1", BigDecimal.ZERO)
        assertNotEquals(cached, fromCache)
    }
}
