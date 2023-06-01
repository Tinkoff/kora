package ru.tinkoff.kora.test.extension.junit5;

import org.jetbrains.annotations.NotNull;
import ru.tinkoff.kora.application.graph.ApplicationGraphDraw;
import ru.tinkoff.kora.common.Tag;

import javax.annotation.Nonnull;
import javax.annotation.Nullable;
import java.lang.reflect.Type;
import java.util.List;

/**
 * {@link ApplicationGraphDraw} abstraction for {@link KoraAppTest}
 */
public interface KoraAppGraph {

    @Nullable
    Object getFirst(@NotNull Type type);

    /**
     * Try to find implementation in Graph by type and tags
     *
     * @param type of component to search
     * @param tags associated with component
     * @return type instance from Graph
     */
    @Nullable
    Object getFirst(@NotNull Type type, Class<?>... tags);

    @Nullable
    <T> T getFirst(@NotNull Class<T> type);

    /**
     * Try to find implementation in Graph by type and tags
     *
     * @param type of component to search
     * @param tags associated with component
     * @param <T>  type parameter
     * @return type instance from Graph
     */
    @Nullable
    <T> T getFirst(@NotNull Class<T> type, Class<?>... tags);

    @Nonnull
    List<Object> getAll(@NotNull Type type);

    /**
     * Try to find implementation in Graph by type using {@link Tag.Any}
     *
     * @param type of component to search
     * @param tags associated with component
     * @return component instance from Graph
     */
    @Nonnull
    List<Object> getAll(@NotNull Type type, Class<?>... tags);

    @Nonnull
    <T> List<T> getAll(@NotNull Class<T> type);

    /**
     * Try to find implementation in Graph by type using {@link Tag.Any}
     *
     * @param type of component to search
     * @param tags associated with component
     * @param <T>  type parameter
     * @return type instance from Graph
     */
    @Nonnull
    <T> List<T> getAll(@NotNull Class<T> type, Class<?>... tags);
}
