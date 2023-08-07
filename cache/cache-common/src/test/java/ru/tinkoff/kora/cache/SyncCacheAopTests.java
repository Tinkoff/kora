package ru.tinkoff.kora.cache;

import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.TestInstance;
import ru.tinkoff.kora.cache.testcache.DummyCache;
import ru.tinkoff.kora.cache.testcache.DummyCacheManager;

@TestInstance(TestInstance.Lifecycle.PER_CLASS)
class SyncCacheAopTests extends Assertions {

    private final DummyCacheManager<String, String> cacheFacade1 = new DummyCacheManager<>();
    private final DummyCache<String, String> cacheFacade2 = new DummyCache<>("test");

    @BeforeEach
    void cleanup() {
        cacheFacade1.reset();
        cacheFacade2.invalidateAll();
    }

    @Test
    void getFromCacheWhenWasCacheEmpty() {
        // given
        final DummyCacheManager<String, String> cacheFacade1 = new DummyCacheManager<>();
        final DummyCache<String, String> cacheFacade2 = new DummyCache<>("test");
        final CacheBuilder<String, String> builder = CacheManager.builder();
        final CacheManager<String, String> service = builder
            .addFacade(cacheFacade1)
            .addFacadeFunction(n -> cacheFacade2)
            .build();

        // when
        // then
        final Cache<String, String> facadeCache = service.getCache("test");
        assertNull(facadeCache.get("key1"));
    }

    @Test
    void getFromCacheForFacade1() {
        // given
        final DummyCacheManager<String, String> cacheFacade1 = new DummyCacheManager<>();
        final DummyCache<String, String> cacheFacade2 = new DummyCache<>("test");
        final CacheBuilder<String, String> builder = CacheManager.builder();
        final CacheManager<String, String> service = builder
            .addFacade(cacheFacade1)
            .addFacadeFunction(name -> cacheFacade2)
            .build();

        // when
        final String result = "value1";
        cacheFacade1.getCache("test").put("key1", result);

        // then
        final Cache<String, String> facadeCache = service.getCache("test");
        assertEquals(result, facadeCache.get("key1"));
    }

    @Test
    void getFromCacheForFacade2() {
        // given
        final DummyCacheManager<String, String> cacheFacade1 = new DummyCacheManager<>();
        final DummyCache<String, String> cacheFacade2 = new DummyCache<>("test");
        final CacheBuilder<String, String> builder = CacheManager.builder();
        final CacheManager<String, String> service = builder
            .addFacade(cacheFacade1)
            .addFacadeFunction(name -> cacheFacade2)
            .build();

        // when
        final String result = "value1";
        cacheFacade2.put("key1", result);

        // then
        final Cache<String, String> facadeCache = service.getCache("test");
        assertEquals(result, facadeCache.get("key1"));
    }

    @Test
    void putCacheForFacade() {
        // given
        final DummyCacheManager<String, String> cacheFacade1 = new DummyCacheManager<>();
        final DummyCache<String, String> cacheFacade2 = new DummyCache<>("test");
        final CacheBuilder<String, String> builder = CacheManager.builder();
        final CacheManager<String, String> service = builder
            .addFacade(cacheFacade1)
            .addFacadeFunction(name -> cacheFacade2)
            .build();

        // when
        final Cache<String, String> facadeCache = service.getCache("test");
        final String result = "value1";
        facadeCache.put("key1", result);

        // then
        assertEquals(result, facadeCache.get("key1"));
        assertEquals(result, cacheFacade1.getCache("test").get("key1"));
        assertEquals(result, cacheFacade2.get("key1"));
    }

    @Test
    void invalidateCacheForFacade() {
        // given
        final DummyCacheManager<String, String> cacheFacade1 = new DummyCacheManager<>();
        final DummyCache<String, String> cacheFacade2 = new DummyCache<>("test");
        final CacheBuilder<String, String> builder = CacheManager.builder();
        final CacheManager<String, String> service = builder
            .addFacade(cacheFacade1)
            .addFacadeFunction(name -> cacheFacade2)
            .build();

        final String result = "value1";
        cacheFacade1.getCache("test").put("key1", result);
        cacheFacade2.put("key1", result);

        // when
        final Cache<String, String> facadeCache = service.getCache("test");
        assertNotNull(facadeCache.get("key1"));
        facadeCache.invalidate("key1");

        // then
        assertNull(facadeCache.get("key1"));
        assertNull(cacheFacade1.getCache("test").get("key1"));
        assertNull(cacheFacade2.get("key1"));
    }

    @Test
    void invalidateAllCacheForFacade() {
        // given
        final DummyCacheManager<String, String> cacheFacade1 = new DummyCacheManager<>();
        final DummyCache<String, String> cacheFacade2 = new DummyCache<>("test");
        final CacheBuilder<String, String> builder = CacheManager.builder();
        final CacheManager<String, String> service = builder
            .addFacade(cacheFacade1)
            .addFacadeFunction(name -> cacheFacade2)
            .build();

        final String result = "value1";
        cacheFacade1.getCache("test").put("key1", result);
        cacheFacade2.put("key1", result);

        // when
        final Cache<String, String> facadeCache = service.getCache("test");
        assertNotNull(facadeCache.get("key1"));
        facadeCache.invalidateAll();

        // then
        assertNull(facadeCache.get("key1"));
        assertNull(cacheFacade1.getCache("test").get("key1"));
        assertNull(cacheFacade2.get("key1"));
    }
}
