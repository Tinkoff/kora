package ru.tinkoff.kora.http.client.common.request;

import reactor.core.publisher.Flux;
import ru.tinkoff.kora.http.common.HttpHeaders;

import javax.annotation.Nonnull;
import java.net.URI;
import java.net.URLEncoder;
import java.nio.ByteBuffer;
import java.time.Duration;
import java.util.*;

import static java.nio.charset.StandardCharsets.UTF_8;

final class HttpClientRequestBuilder implements HttpClientRequest.Builder {
    private final String method;
    private String uriTemplate;
    private Map<String, String> pathParams = new LinkedHashMap<>();
    private Map<String, List<String>> queryParams = new LinkedHashMap<>();
    private Map<String, List<String>> headers = new LinkedHashMap<>();
    private Flux<ByteBuffer> body = Flux.empty();
    private int requestTimeout = -1;

    HttpClientRequestBuilder(String method, String uriTemplate) {
        this.method = method;
        this.uriTemplate = uriTemplate;
    }

    HttpClientRequestBuilder(HttpClientRequest httpClientRequest) {
        this.method = httpClientRequest.method();
        this.uriTemplate = httpClientRequest.uriTemplate();
        this.pathParams = httpClientRequest.pathParams();
        this.queryParams = httpClientRequest.queryParams();
        this.headers = fromHeaders(httpClientRequest.headers());
        this.body = httpClientRequest.body();
        this.requestTimeout = httpClientRequest.requestTimeout();
    }

    @Override
    public HttpClientRequestBuilder uriTemplate(String uriTemplate) {
        this.uriTemplate = uriTemplate;
        return this;
    }

    @Override
    public HttpClientRequestBuilder queryParam(String name, Integer value) {
        if (value == null) {
            return queryParam(name);
        }
        return queryParamNotNull(name, Integer.toString(value));
    }

    @Override
    public HttpClientRequestBuilder queryParam(String name, Long value) {
        if (value == null) {
            return queryParam(name);
        }
        return queryParamNotNull(name, Long.toString(value));
    }

    @Override
    public HttpClientRequestBuilder queryParam(String name, Boolean value) {
        if (value == null) {
            return queryParam(name);
        }
        return queryParamNotNull(name, Boolean.toString(value));
    }

    @Override
    public HttpClientRequestBuilder queryParam(String name, String value) {
        if (value == null) {
            return queryParam(name);
        }
        return queryParamNotNull(name, value);
    }

    @Override
    public HttpClientRequestBuilder queryParam(String name, UUID value) {
        if (value == null) {
            return queryParam(name);
        }
        return queryParamNotNull(name, value.toString());
    }

    @Override
    public HttpClientRequestBuilder queryParam(String name) {
        this.queryParams.computeIfAbsent(name, k -> new ArrayList<>());
        return this;
    }

    @Override
    public HttpClientRequestBuilder queryParam(String name, Collection<String> values) {
        if (values == null || values.isEmpty()) {
            return queryParam(name);
        }

        this.queryParams.compute(name, (k, v) -> {
            if (v == null) {
                return new ArrayList<>(values);
            } else {
                v.addAll(values);
                return v;
            }
        });
        return this;
    }


    private HttpClientRequestBuilder queryParamNotNull(String name, @Nonnull String value) {
        this.queryParams.compute(name, (k, v) -> {
            if (v == null) {
                final List<String> values = new ArrayList<>(1);
                values.add(value);
                return values;
            } else {
                v.add(value);
                return v;
            }
        });
        return this;
    }

    @Override
    public HttpClientRequestBuilder pathParam(String name, @Nonnull String value) {
        this.pathParams.put(name, value);
        return this;
    }

    @Override
    public HttpClientRequestBuilder pathParam(String name, @Nonnull Collection<String> value) {
        this.pathParams.put(name, String.join(",", value));
        return this;
    }

    @Override
    public HttpClientRequestBuilder pathParam(String name, int value) {
        this.pathParams.put(name, String.valueOf(value));
        return this;
    }

    @Override
    public HttpClientRequestBuilder pathParam(String name, long value) {
        this.pathParams.put(name, String.valueOf(value));
        return this;
    }

    @Override
    public HttpClientRequestBuilder pathParam(String name, boolean value) {
        this.pathParams.put(name, String.valueOf(value));
        return this;
    }

    @Override
    public HttpClientRequestBuilder pathParam(String name, @Nonnull UUID value) {
        this.pathParams.put(name, value.toString());
        return this;
    }

    @Override
    public HttpClientRequestBuilder header(String name, String value) {
        var headers = this.headers.computeIfAbsent(name.toLowerCase(), k -> new ArrayList<>(1));
        headers.add(value);
        return this;
    }

    @Override
    public HttpClientRequestBuilder requestTimeout(int requestTimeout) {
        this.requestTimeout = requestTimeout;
        return this;
    }

    @Override
    public HttpClientRequestBuilder requestTimeout(Duration requestTimeout) {
        this.requestTimeout = requestTimeout.toMillisPart();
        return this;
    }

    @Override
    public HttpClientRequestBuilder body(Flux<ByteBuffer> body) {
        this.body = body;
        return this;
    }

    @Override
    public HttpClientRequestBuilder body(ByteBuffer body) {
        this.body = Flux.just(body);
        return this;
    }

    @Override
    public HttpClientRequestBuilder body(byte[] body) {
        this.body = Flux.just(ByteBuffer.wrap(body));
        return this;
    }

    @Override
    public HttpClientRequestBuilder headers(HttpHeaders headers) {
        this.headers = fromHeaders(headers);
        return this;
    }

    @Override
    public HttpClientRequest build() {
        var resolvedUri = resolveUri(this.uriTemplate, this.pathParams, this.queryParams);
        var uri = URI.create(resolvedUri);
        var authority = uri.getAuthority();
        var operation = operation(this.method, this.uriTemplate, uri);

        return new HttpClientRequestImpl(
            this.method, this.uriTemplate, resolvedUri, Map.copyOf(this.queryParams), Map.copyOf(this.pathParams), toHeaders(this.headers), this.body, this.requestTimeout, authority, operation
        );
    }

    private static String resolveUri(String uriTemplate, Map<String, String> pathParams, Map<String, List<String>> queryParams) {
        var template = uriTemplate;
        if (!pathParams.isEmpty()) {
            for (var entry : pathParams.entrySet()) {
                template = template.replace("{" + entry.getKey() + "}", URLEncoder.encode(entry.getValue(), UTF_8));
            }
        }

        if (queryParams.isEmpty()) {
            return template;
        }

        var delimeter = template.contains("?")
            ? (template.endsWith("?") ? "" : "&")
            : "?";

        var sb = new StringBuilder(template)
            .append(delimeter);

        boolean isFirstQueryParam = true;
        for (var entry : queryParams.entrySet()) {
            for (String value : entry.getValue()) {
                if (isFirstQueryParam) {
                    sb.append('=');
                    isFirstQueryParam = false;
                } else {
                    sb.append('&');
                }

                sb.append(URLEncoder.encode(entry.getKey(), UTF_8))
                    .append('=').append(URLEncoder.encode(value, UTF_8));
            }
        }

        return sb.toString();
    }

    private static String operation(String method, String uriTemplate, URI uri) {
        if (uri.getAuthority() != null) {
            if (uri.getScheme() != null) {
                uriTemplate = uriTemplate.replace(uri.getScheme() + "://" + uri.getAuthority(), "");
            }
        }
        var questionMark = uriTemplate.indexOf('?');
        if (questionMark >= 0) {
            uriTemplate = uriTemplate.substring(0, questionMark);
        }
        return method + " " + uriTemplate;
    }

    private static HashMap<String, List<String>> fromHeaders(HttpHeaders headers) {
        var collect = new HashMap<String, List<String>>(headers.size());
        for (var e : headers) {
            collect.putIfAbsent(e.getKey(), new ArrayList<>(e.getValue()));
        }
        return collect;
    }

    @SuppressWarnings("unchecked")
    private static HttpHeaders toHeaders(Map<String, List<String>> map) {
        return HttpHeaders.of(map.entrySet().toArray(Map.Entry[]::new));
    }
}
