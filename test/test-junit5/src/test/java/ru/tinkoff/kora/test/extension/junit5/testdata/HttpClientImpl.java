package ru.tinkoff.kora.test.extension.junit5.testdata;

import ru.tinkoff.kora.http.client.common.annotation.HttpClient;
import ru.tinkoff.kora.http.common.HttpMethod;
import ru.tinkoff.kora.http.common.annotation.HttpRoute;

@HttpClient(configPath = "httpClient.default")
public interface HttpClientImpl {

    @HttpRoute(method = HttpMethod.POST, path = "/void")
    void post();
}
