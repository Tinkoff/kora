package ru.tinkoff.kora.test.extension.junit5;

import org.jetbrains.annotations.NotNull;

import javax.annotation.Nullable;
import java.lang.reflect.Type;

public interface KoraAppGraph {

    /**
     * Try to find implementation in Graph by type
     *
     * @param type to look for
     * @param <T>  type parameter
     * @return type instance from Graph
     */
    @Nullable
    <T> T get(@NotNull Type type);

    /**
     * Try to find implementation in Graph by type and tags
     *
     * @param type to look for
     * @param tags associated with type
     * @param <T>  type parameter
     * @return type instance from Graph
     */
    @Nullable
    <T> T get(@NotNull Type type, Class<?>... tags);
}
