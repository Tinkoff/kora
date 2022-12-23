package ru.tinkoff.kora.example;

import reactor.core.publisher.Mono;
import ru.tinkoff.kora.application.graph.Lifecycle;
import ru.tinkoff.kora.http.client.common.annotation.HttpClient;
import ru.tinkoff.kora.http.common.HttpMethod;
import ru.tinkoff.kora.http.common.annotation.HttpRoute;

@HttpClient(configPath = "httpClient.default")
public interface TestHttpClient extends Lifecycle {

    @HttpRoute(method = HttpMethod.POST, path = "/void")
    void sync();

    @Override
    default Mono<?> init() {
        return Mono.empty();
    }

    @Override
    default Mono<?> release() {
        return Mono.empty();
    }
}
