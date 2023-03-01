package ru.tinkoff.kora.test.extension.junit5;

import org.jetbrains.annotations.NotNull;
import ru.tinkoff.kora.common.Tag;

import javax.annotation.Nonnull;
import javax.annotation.Nullable;
import java.lang.reflect.Type;
import java.util.List;

public interface KoraAppGraph {

    /**
     * Try to find implementation in Graph by type without Tags
     *
     * @param type to look for
     * @param <T>  type parameter
     * @return type instance from Graph
     */
    @Nullable
    <T> T get(@NotNull Type type);

    /**
     * Try to find implementation in Graph by type without Tags
     *
     * @param type to look for
     * @param <T>  type parameter
     * @return type instance from Graph
     */
    @Nullable
    <T> T get(@NotNull Class<T> type);

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

    /**
     * Try to find implementation in Graph by type and tags
     *
     * @param type to look for
     * @param tags associated with type
     * @param <T>  type parameter
     * @return type instance from Graph
     */
    @Nullable
    <T> T get(@NotNull Class<T> type, Class<?>... tags);

    /**
     * Try to find implementation in Graph by type using {@link Tag.Any}
     *
     * @param type to look for
     * @param <T>  type parameter
     * @return type instance from Graph
     */
    @Nonnull
    <T> List<T> getAny(@NotNull Type type);

    /**
     * Try to find implementation in Graph by type using {@link Tag.Any}
     *
     * @param type to look for
     * @param <T>  type parameter
     * @return type instance from Graph
     */
    @Nonnull
    <T> List<T> getAny(@NotNull Class<T> type);
}
