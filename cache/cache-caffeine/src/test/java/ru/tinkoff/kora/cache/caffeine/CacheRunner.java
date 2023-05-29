package ru.tinkoff.kora.cache.caffeine;

import org.junit.jupiter.api.Assertions;
import ru.tinkoff.kora.annotation.processor.common.TestUtils;
import ru.tinkoff.kora.aop.annotation.processor.AopAnnotationProcessor;
import ru.tinkoff.kora.application.graph.ApplicationGraphDraw;
import ru.tinkoff.kora.cache.annotation.processor.CacheAnnotationProcessor;
import ru.tinkoff.kora.cache.caffeine.testdata.DummyCache;
import ru.tinkoff.kora.config.annotation.processor.processor.ConfigRootAnnotationProcessor;
import ru.tinkoff.kora.config.annotation.processor.processor.ConfigSourceAnnotationProcessor;
import ru.tinkoff.kora.kora.app.annotation.processor.KoraAppProcessor;

import java.lang.reflect.Constructor;
import java.util.ArrayList;
import java.util.List;
import java.util.function.Supplier;

abstract class CacheRunner extends Assertions implements CaffeineCacheModule {

    protected DummyCache createCache() {
        try {
            return new DummyCache(new CaffeineCacheConfig(null, null, null, null),
                caffeineCacheFactory(), defaultCacheTelemetry(null, null));
        } catch (Exception e) {
            throw new IllegalStateException(e);
        }
    }
}
