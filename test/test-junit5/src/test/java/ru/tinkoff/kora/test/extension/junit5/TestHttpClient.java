package ru.tinkoff.kora.test.extension.junit5;

import reactor.core.publisher.Mono;
import ru.tinkoff.kora.annotation.processor.common.MockLifecycle;
import ru.tinkoff.kora.common.Component;
import ru.tinkoff.kora.http.client.common.annotation.HttpClient;
import ru.tinkoff.kora.http.common.HttpMethod;
import ru.tinkoff.kora.http.common.annotation.HttpRoute;
import ru.tinkoff.kora.json.common.annotation.Json;

@HttpClient(configPath = "httpClient.default")
public interface TestHttpClient extends MockLifecycle {

    @HttpRoute(method = HttpMethod.POST, path = "/void")
    void sync();
}
