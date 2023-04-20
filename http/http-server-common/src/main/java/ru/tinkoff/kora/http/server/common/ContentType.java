package ru.tinkoff.kora.http.server.common;

import javax.annotation.Nonnull;
import java.nio.charset.Charset;

public enum ContentType {

    TEXT_JSON("text/json"),
    TEXT_PLAIN("text/plain"),
    TEXT_PLAIN_UTF_8("text/plain; charset=utf-8"),
    APPLICATION_JSON("application/json"),
    APPLICATION_JSON_UTF_8("application/json; charset=utf-8"),
    APPLICATION_YAML("application/x-yaml"),
    APPLICATION_YAML_UTF_8("application/x-yaml; charset=utf-8"),
    APPLICATION_OCTET_STREAM("application/octet-stream");

    private final String value;

    ContentType(String value) {
        this.value = value;
    }

    @Nonnull
    public String value() {
        return value;
    }

    @Nonnull
    public String charset(Charset charset) {
        return value + "; charset=" + charset.name();
    }

    @Nonnull
    @Override
    public String toString() {
        return value;
    }
}
