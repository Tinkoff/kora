package ru.tinkoff.kora.http.server.common;

import reactor.core.publisher.Flux;
import ru.tinkoff.kora.http.common.HttpHeaders;

import javax.annotation.Nullable;
import java.nio.ByteBuffer;

import static java.nio.charset.StandardCharsets.UTF_8;

public class HttpServerResponseException extends RuntimeException implements HttpServerResponse {

    private final HttpServerResponse response;

    public HttpServerResponseException(String message, int code) {
        super(message);
        this.response = HttpServerResponse.of(code, ContentType.TEXT_PLAIN_UTF_8, UTF_8.encode(message));
    }

    public HttpServerResponseException(String message, Throwable cause, int code) {
        super(message, cause);
        this.response = HttpServerResponse.of(code, ContentType.TEXT_PLAIN_UTF_8, UTF_8.encode(message));
    }

    public HttpServerResponseException(String message, HttpServerResponse response) {
        super(message);
        this.response = response;
    }

    public HttpServerResponseException(String message, Throwable cause, HttpServerResponse response) {
        super(message, cause);
        this.response = response;
    }

    @Override
    public int code() {
        return response.code();
    }

    @Override
    public int contentLength() {
        return response.contentLength();
    }

    @Override
    public String contentType() {
        return response.contentType();
    }

    @Override
    public HttpHeaders headers() {
        return response.headers();
    }

    @Override
    public Flux<? extends ByteBuffer> body() {
        return response.body();
    }

    @Override
    public String toString() {
        return response.toString();
    }
}
