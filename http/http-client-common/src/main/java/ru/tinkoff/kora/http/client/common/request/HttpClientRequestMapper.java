package ru.tinkoff.kora.http.client.common.request;

import ru.tinkoff.kora.common.Mapping;

public interface HttpClientRequestMapper<T> extends Mapping.MappingFunction {

    record Request<T>(HttpClientRequest.Builder builder, T parameter) {}

    HttpClientRequest.Builder apply(Request<T> request);
}
