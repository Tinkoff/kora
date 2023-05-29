package ru.tinkoff.kora.cache.annotation.processor.testcache;

import reactor.core.publisher.Mono;
import ru.tinkoff.kora.cache.LoadableCache;
import ru.tinkoff.kora.cache.annotation.Cache;
import ru.tinkoff.kora.cache.caffeine.CaffeineCache;

import javax.annotation.Nonnull;
import java.util.HashMap;
import java.util.Map;

@Cache("dummy")
public interface DummyCache1 extends CaffeineCache<String, String> {

}
