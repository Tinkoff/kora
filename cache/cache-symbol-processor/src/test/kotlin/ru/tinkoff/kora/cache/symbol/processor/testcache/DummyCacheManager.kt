package ru.tinkoff.kora.cache.symbol.processor.testcache

class DummyCacheManager<K, V> {

    private val cacheMap = HashMap<String, DummyCache<K, V>>()

    override fun getCache(name: String): DummyCache<K, V> {
        return cacheMap.computeIfAbsent(name) { DummyCache(name) }
    }

    fun reset() {
        cacheMap.values.forEach { c -> c.reset() }
    }
}
