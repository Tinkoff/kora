package ru.tinkoff.kora.http.server.common;

import reactor.core.publisher.Flux;
import reactor.core.publisher.Mono;
import ru.tinkoff.kora.http.common.HttpHeaders;

import javax.annotation.Nullable;
import java.nio.ByteBuffer;

record HttpServerResponseImpl(int code,
                              HttpHeaders headers,
                              String contentType,
                              int contentLength,
                              Flux<? extends ByteBuffer> body) implements HttpServerResponse {

    @Override
    public String toString() {
        return "HttpServerResponse{code=" + code
               + ", contentType=" + contentType
               + ", contentLength=" + contentLength
               + ", headers=" + headers
               + '}';
    }
}
