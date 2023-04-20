package ru.tinkoff.kora.http.server.common;

import reactor.core.publisher.Flux;
import reactor.core.publisher.Mono;
import ru.tinkoff.kora.http.common.HttpHeaders;

import java.nio.ByteBuffer;

public interface HttpServerResponse {

    int code();

    int contentLength();

    String contentType();

    HttpHeaders headers();

    Flux<? extends ByteBuffer> body();

    static HttpServerResponse of(int code, String contentType) {
        return new HttpServerResponseImpl(code, HttpHeaders.of(), contentType, 0, Flux.empty());
    }

    static HttpServerResponse of(int code, ContentType contentType) {
        return new HttpServerResponseImpl(code, HttpHeaders.of(), contentType.value(), 0, Flux.empty());
    }

    static HttpServerResponse of(int code, String contentType, HttpHeaders headers) {
        return new HttpServerResponseImpl(code, headers, contentType, 0, Flux.empty());
    }

    static HttpServerResponse of(int code, ContentType contentType, HttpHeaders headers) {
        return new HttpServerResponseImpl(code, headers, contentType.value(), 0, Flux.empty());
    }

    static HttpServerResponse of(int code, String contentType, byte[] body) {
        return new HttpServerResponseImpl(code, HttpHeaders.of(), contentType, body.length, Flux.just(ByteBuffer.wrap(body)));
    }

    static HttpServerResponse of(int code, ContentType contentType, byte[] body) {
        return new HttpServerResponseImpl(code, HttpHeaders.of(), contentType.value(), body.length, Flux.just(ByteBuffer.wrap(body)));
    }

    static HttpServerResponse of(int code, String contentType, ByteBuffer body) {
        return new HttpServerResponseImpl(code, HttpHeaders.of(), contentType, body.remaining(), Mono.fromCallable(body::slice).flux());
    }

    static HttpServerResponse of(int code, ContentType contentType, ByteBuffer body) {
        return new HttpServerResponseImpl(code, HttpHeaders.of(), contentType.value(), body.remaining(), Mono.fromCallable(body::slice).flux());
    }

    static HttpServerResponse of(int code, String contentType, HttpHeaders headers, byte[] body) {
        return new HttpServerResponseImpl(code, headers, contentType, body.length, Flux.just(ByteBuffer.wrap(body)));
    }

    static HttpServerResponse of(int code, ContentType contentType, HttpHeaders headers, byte[] body) {
        return new HttpServerResponseImpl(code, headers, contentType.value(), body.length, Flux.just(ByteBuffer.wrap(body)));
    }

    static HttpServerResponse of(int code, String contentType, HttpHeaders headers, ByteBuffer body) {
        return new HttpServerResponseImpl(code, headers, contentType, body.remaining(), Mono.fromCallable(body::slice).flux());
    }

    static HttpServerResponse of(int code, ContentType contentType, HttpHeaders headers, ByteBuffer body) {
        return new HttpServerResponseImpl(code, headers, contentType.value(), body.remaining(), Mono.fromCallable(body::slice).flux());
    }

    static HttpServerResponse of(int code, String contentType, HttpHeaders headers, int contentLength, Flux<? extends ByteBuffer> body) {
        return new HttpServerResponseImpl(code, headers, contentType, contentLength, body);
    }

    static HttpServerResponse of(int code, ContentType contentType, HttpHeaders headers, int contentLength, Flux<? extends ByteBuffer> body) {
        return new HttpServerResponseImpl(code, headers, contentType.value(), contentLength, body);
    }
}
