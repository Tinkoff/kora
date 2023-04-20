package ru.tinkoff.kora.http.client.common.form;

import ru.tinkoff.kora.http.client.common.request.HttpClientRequest;
import ru.tinkoff.kora.http.client.common.request.HttpClientRequestMapper;
import ru.tinkoff.kora.http.common.form.FormMultipart;

public final class FormMultipartClientRequestMapper implements HttpClientRequestMapper<FormMultipart> {
    @Override
    public HttpClientRequest.Builder apply(Request<FormMultipart> request) {
        return MultipartWriter.write(request.builder(), request.parameter().parts());
    }
}
