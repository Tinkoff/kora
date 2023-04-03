package ru.tinkoff.kora.http.server.common;

import reactor.core.publisher.Flux;
import ru.tinkoff.kora.http.common.HttpHeaders;

import java.nio.ByteBuffer;
import java.util.Collection;
import java.util.Map;

public record SimpleHttpServerRequest(String method, String path, HttpHeaders headers, Map<String, ? extends Collection<String>> queryParams, Map<String, String> pathParams, Flux<ByteBuffer> body) implements HttpServerRequest {

    public SimpleHttpServerRequest(HttpServerRequest request, HttpHeaders headers) {
        this(
                request.method(),
                request.path(),
                headers,
                request.queryParams(),
                request.pathParams(),
                request.body()
        );
    }

}
