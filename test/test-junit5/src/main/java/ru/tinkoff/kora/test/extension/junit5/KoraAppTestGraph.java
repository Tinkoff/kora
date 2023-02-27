package ru.tinkoff.kora.test.extension.junit5;

import org.jetbrains.annotations.NotNull;

import javax.annotation.Nullable;

public interface KoraAppTestGraph {

    @Nullable
    <T> T get(@NotNull Class<T> type);

    @Nullable
    <T> T get(@NotNull Class<T> type, Class<?>... tags);
}
