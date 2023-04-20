package ru.tinkoff.kora.http.server.common;

import reactor.core.publisher.Flux;
import ru.tinkoff.kora.http.common.HttpHeaders;

import java.nio.ByteBuffer;
import java.util.List;
import java.util.Map;

public interface HttpServerRequest {

    String method();

    String path();

    Map<String, List<String>> queryParams();

    Map<String, String> pathParams();

    HttpHeaders headers();

    Flux<ByteBuffer> body();
}
