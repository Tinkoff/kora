package ru.tinkoff.kora.http.client.common.request;

import reactor.core.publisher.Flux;
import ru.tinkoff.kora.http.common.HttpHeaders;
import ru.tinkoff.kora.http.common.HttpMethod;

import javax.annotation.Nonnull;
import java.nio.ByteBuffer;
import java.time.Duration;
import java.util.Collection;
import java.util.List;
import java.util.Map;
import java.util.UUID;

public interface HttpClientRequest {

    String method();

    String uriTemplate();

    String uriResolved();

    Map<String, List<String>> queryParams();

    Map<String, String> pathParams();

    HttpHeaders headers();

    Flux<ByteBuffer> body();

    String authority();

    String operation();

    /**
     * @return request timeout in millis
     */
    int requestTimeout();

    default Builder toBuilder() {
        return new HttpClientRequestBuilder(this);
    }

    static Builder get(String path) {
        return new HttpClientRequestBuilder(HttpMethod.GET, path);
    }

    static Builder head(String path) {
        return new HttpClientRequestBuilder(HttpMethod.HEAD, path);
    }

    static Builder post(String path) {
        return new HttpClientRequestBuilder(HttpMethod.POST, path);
    }

    static Builder put(String path) {
        return new HttpClientRequestBuilder(HttpMethod.PUT, path);
    }

    static Builder delete(String path) {
        return new HttpClientRequestBuilder(HttpMethod.DELETE, path);
    }

    static Builder connect(String path) {
        return new HttpClientRequestBuilder(HttpMethod.CONNECT, path);
    }

    static Builder options(String path) {
        return new HttpClientRequestBuilder(HttpMethod.OPTIONS, path);
    }

    static Builder trace(String path) {
        return new HttpClientRequestBuilder(HttpMethod.TRACE, path);
    }

    static Builder patch(String path) {
        return new HttpClientRequestBuilder(HttpMethod.PATCH, path);
    }

    static Builder of(String method, String path) {
        return new HttpClientRequestBuilder(method, path);
    }

    interface Builder {

        Builder uriTemplate(String uriTemplate);

        Builder queryParam(String name);

        Builder queryParam(String name, String value);

        Builder queryParam(String name, Collection<String> value);

        Builder queryParam(String name, Integer value);

        Builder queryParam(String name, Long value);

        Builder queryParam(String name, Boolean value);

        Builder queryParam(String name, UUID value);

        Builder pathParam(String name, @Nonnull String value);

        Builder pathParam(String name, @Nonnull Collection<String> value);

        Builder pathParam(String name, int value);

        Builder pathParam(String name, long value);

        Builder pathParam(String name, boolean value);

        Builder pathParam(String name, @Nonnull UUID value);

        Builder header(String name, String value);

        Builder requestTimeout(int timeoutInMillis);

        Builder requestTimeout(Duration requestTimeout);

        Builder body(Flux<ByteBuffer> body);

        Builder body(ByteBuffer body);

        Builder body(byte[] body);

        Builder headers(HttpHeaders headers);

        HttpClientRequest build();
    }
}
