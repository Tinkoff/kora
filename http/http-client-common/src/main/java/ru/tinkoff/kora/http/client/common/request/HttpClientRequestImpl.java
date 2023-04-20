package ru.tinkoff.kora.http.client.common.request;

import reactor.core.publisher.Flux;
import ru.tinkoff.kora.http.common.HttpHeaders;

import java.nio.ByteBuffer;
import java.util.List;
import java.util.Map;

record HttpClientRequestImpl(String method,
                             String uriTemplate,
                             String uriResolved,
                             Map<String, List<String>> queryParams,
                             Map<String, String> pathParams,
                             HttpHeaders headers,
                             Flux<ByteBuffer> body,
                             int requestTimeout,
                             String authority,
                             String operation) implements HttpClientRequest {

    @Override
    public String toString() {
        return method.toUpperCase() + " " + uriResolved;
    }
}
