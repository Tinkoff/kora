package ru.tinkoff.kora.test.extension.junit5.testdata;

import reactor.core.publisher.Mono;
import ru.tinkoff.kora.application.graph.Lifecycle;
import ru.tinkoff.kora.common.Component;

public interface ReplaceComponent extends Lifecycle {

    String get();

    @Override
    default Mono<?> init() {
        return Mono.empty();
    }

    @Override
    default Mono<?> release() {
        return Mono.empty();
    }
}
