package ru.tinkoff.kora.test.extension.junit5;

import org.jetbrains.annotations.NotNull;

import javax.annotation.Nullable;

public interface KoraAppTestGraph {

    /**
     * Try to find implementation in Graph by type
     *
     * @param type to look for
     * @return type instance from Graph
     * @param <T> type parameter
     */
    @Nullable
    <T> T get(@NotNull Class<T> type);

    /**
     * Try to find implementation in Graph by type and tags
     *
     * @param type to look for
     * @param tags associated with type
     * @return type instance from Graph
     * @param <T> type parameter
     */
    @Nullable
    <T> T get(@NotNull Class<T> type, Class<?>... tags);
}
