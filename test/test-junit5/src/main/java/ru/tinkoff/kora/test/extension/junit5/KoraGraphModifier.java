package ru.tinkoff.kora.test.extension.junit5;

import org.jetbrains.annotations.NotNull;

import java.util.ArrayList;
import java.util.List;
import java.util.Objects;
import java.util.function.Function;
import java.util.function.Supplier;

public final class KoraGraphModifier {

    public record NodeAddition(Function<KoraAppTestGraph, ?> function, Class<?>[] tags) {}

    public record NodeReplacement(Function<KoraAppTestGraph, ?> function, NodeToReplace replacement) {

        public record NodeToReplace(Class<?> type, Class<?>[] tags) {}
    }

    public record NodeMock(Class<?> type, Class<?>[] tags) {}

    private final List<NodeAddition> additions = new ArrayList<>();
    private final List<NodeReplacement> replacements = new ArrayList<>();
    private final List<NodeMock> mocks = new ArrayList<>();

    List<NodeAddition> getAdditions() {
        return additions;
    }

    List<NodeReplacement> getReplacements() {
        return replacements;
    }

    List<NodeMock> getMocks() {
        return mocks;
    }

    /**
     * Component that should be added to Graph Context
     */
    public <T> KoraGraphModifier addComponent(@NotNull Supplier<T> instanceSupplier) {
        additions.add(new NodeAddition(s -> instanceSupplier.get(), null));
        return this;
    }

    /**
     * Component that should be added to Graph Context
     */
    public <T> KoraGraphModifier addComponent(@NotNull Supplier<T> instanceSupplier,
                                              Class<?>... tags) {
        if (tags == null || tags.length == 0) {
            return addComponent(instanceSupplier);
        } else {
            additions.add(new NodeAddition(s -> instanceSupplier.get(), tags));
            return this;
        }
    }

    /**
     * Component that should be added to Graph Context
     */
    public <T> KoraGraphModifier addComponent(@NotNull Function<KoraAppTestGraph, T> instanceSupplier) {
        additions.add(new NodeAddition(instanceSupplier, null));
        return this;
    }

    /**
     * Component that should be added to Graph Context
     */
    public <T> KoraGraphModifier addComponent(@NotNull Function<KoraAppTestGraph, T> instanceSupplier,
                                              Class<?>... tags) {
        if (tags == null || tags.length == 0) {
            return addComponent(instanceSupplier);
        } else {
            additions.add(new NodeAddition(instanceSupplier, tags));
            return this;
        }
    }

    /**
     * Component that should replace existing one with new one
     */
    public <T> KoraGraphModifier replaceComponent(@NotNull Supplier<? extends T> replacementSupplier,
                                                  @NotNull Class<? extends T> typeToReplace) {
        replacements.add(new NodeReplacement(g -> replacementSupplier.get(), new NodeReplacement.NodeToReplace(typeToReplace, null)));
        return this;
    }

    /**
     * Component that should replace existing one with new one
     */
    public <T> KoraGraphModifier replaceComponent(@NotNull Supplier<? extends T> replacementSupplier,
                                                  @NotNull Class<? extends T> typeToReplace,
                                                  Class<?>... tags) {
        if (tags == null || tags.length == 0) {
            return replaceComponent(replacementSupplier, typeToReplace);
        } else {
            replacements.add(new NodeReplacement(g -> replacementSupplier.get(), new NodeReplacement.NodeToReplace(typeToReplace, tags)));
            return this;
        }
    }

    /**
     * Component that should replace existing one with new one
     */
    public <T> KoraGraphModifier replaceComponent(@NotNull Function<KoraAppTestGraph, ? extends T> replacementSupplier,
                                                  @NotNull Class<? extends T> typeToReplace) {
        replacements.add(new NodeReplacement(replacementSupplier, new NodeReplacement.NodeToReplace(typeToReplace, null)));
        return this;
    }

    /**
     * Component that should replace existing one with new one
     */
    public <T> KoraGraphModifier replaceComponent(@NotNull Function<KoraAppTestGraph, ? extends T> replacementSupplier,
                                                  @NotNull Class<? extends T> typeToReplace,
                                                  Class<?>... tags) {
        if (tags == null || tags.length == 0) {
            return replaceComponent(replacementSupplier, typeToReplace);
        } else {
            replacements.add(new NodeReplacement(replacementSupplier, new NodeReplacement.NodeToReplace(typeToReplace, tags)));
            return this;
        }
    }

    /**
     * Component that should be Mocked with {@link org.mockito.Mockito}
     */
    public <T> KoraGraphModifier mockComponent(@NotNull Class<? extends T> typeToMock) {
        mocks.add(new NodeMock(typeToMock, null));
        return this;
    }

    /**
     * Component that should be Mocked with {@link org.mockito.Mockito}
     */
    public <T> KoraGraphModifier mockComponent(@NotNull Class<? extends T> typeToMock,
                                               Class<?>... tags) {
        if (tags == null || tags.length == 0) {
            return mockComponent(typeToMock);
        } else {
            mocks.add(new NodeMock(typeToMock, tags));
            return this;
        }
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (!(o instanceof KoraGraphModifier that)) return false;
        return Objects.equals(additions, that.additions) && Objects.equals(replacements, that.replacements) && Objects.equals(mocks, that.mocks);
    }

    @Override
    public int hashCode() {
        return Objects.hash(additions, replacements, mocks);
    }
}
