package ru.tinkoff.kora.test.extension.junit5;

import org.jetbrains.annotations.NotNull;

import java.lang.reflect.Type;
import java.util.ArrayList;
import java.util.List;
import java.util.Objects;
import java.util.function.Function;
import java.util.function.Supplier;

public final class KoraGraphModification {

    record NodeAddition(Function<KoraAppGraph, ?> function, NodeTypeCandidate candidate) {}

    record NodeReplacement(Function<KoraAppGraph, ?> function, NodeTypeCandidate candidate) {}

    record NodeMock(NodeClassCandidate candidate) {}

    record NodeClassCandidate(Class<?> type, Class<?>[] tags) {}

    record NodeTypeCandidate(Type type, Class<?>[] tags) {}

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
    public <T> KoraGraphModification addComponent(@NotNull Supplier<T> instanceSupplier,
                                                  @NotNull Type typeToAdd) {
        additions.add(new NodeAddition(s -> instanceSupplier.get(), new NodeTypeCandidate(typeToAdd, null)));
        return this;
    }

    /**
     * Component that should be added to Graph Context
     */
    public <T> KoraGraphModification addComponent(@NotNull Supplier<T> instanceSupplier,
                                                  @NotNull Type typeToAdd,
                                                  Class<?>... tags) {
        if (tags == null || tags.length == 0) {
            return addComponent(instanceSupplier, typeToAdd);
        } else {
            additions.add(new NodeAddition(s -> instanceSupplier.get(), new NodeTypeCandidate(typeToAdd, tags)));
            return this;
        }
    }

    /**
     * Component that should be added to Graph Context
     */
    public <T> KoraGraphModification addComponent(@NotNull Function<KoraAppGraph, T> instanceSupplier,
                                                  @NotNull Type typeToAdd) {
        additions.add(new NodeAddition(instanceSupplier, new NodeTypeCandidate(typeToAdd, null)));
        return this;
    }

    /**
     * Component that should be added to Graph Context
     */
    public <T> KoraGraphModification addComponent(@NotNull Function<KoraAppGraph, T> instanceSupplier,
                                                  @NotNull Type typeToAdd,
                                                  Class<?>... tags) {
        if (tags == null || tags.length == 0) {
            return addComponent(instanceSupplier, typeToAdd);
        } else {
            additions.add(new NodeAddition(instanceSupplier, new NodeTypeCandidate(typeToAdd, tags)));
            return this;
        }
    }

    /**
     * Component that should replace existing one with new one
     */
    public <T> KoraGraphModification replaceComponent(@NotNull Supplier<? extends T> replacementSupplier,
                                                      @NotNull Type typeToReplace) {
        replacements.add(new NodeReplacement(g -> replacementSupplier.get(), new NodeTypeCandidate(typeToReplace, null)));
        return this;
    }

    /**
     * Component that should replace existing one with new one
     */
    public <T> KoraGraphModification replaceComponent(@NotNull Supplier<? extends T> replacementSupplier,
                                                      @NotNull Type typeToReplace,
                                                      Class<?>... tags) {
        if (tags == null || tags.length == 0) {
            return replaceComponent(replacementSupplier, typeToReplace);
        } else {
            replacements.add(new NodeReplacement(g -> replacementSupplier.get(), new NodeTypeCandidate(typeToReplace, tags)));
            return this;
        }
    }

    /**
     * Component that should replace existing one with new one
     */
    public <T> KoraGraphModification replaceComponent(@NotNull Function<KoraAppGraph, ? extends T> replacementSupplier,
                                                      @NotNull Type typeToReplace) {
        replacements.add(new NodeReplacement(replacementSupplier, new NodeTypeCandidate(typeToReplace, null)));
        return this;
    }

    /**
     * Component that should replace existing one with new one
     */
    public <T> KoraGraphModification replaceComponent(@NotNull Function<KoraAppGraph, ? extends T> replacementSupplier,
                                                      @NotNull Type typeToReplace,
                                                      Class<?>... tags) {
        if (tags == null || tags.length == 0) {
            return replaceComponent(replacementSupplier, typeToReplace);
        } else {
            replacements.add(new NodeReplacement(replacementSupplier, new NodeTypeCandidate(typeToReplace, tags)));
            return this;
        }
    }

    /**
     * Component that should be Mocked with {@link org.mockito.Mockito}
     */
    public KoraGraphModification mockComponent(@NotNull Class<?> typeToMock) {
        mocks.add(new NodeMock(new NodeClassCandidate(typeToMock, null)));
        return this;
    }

    /**
     * Component that should be Mocked with {@link org.mockito.Mockito}
     */
    public KoraGraphModification mockComponent(@NotNull Class<?> typeToMock,
                                               Class<?>... tags) {
        if (tags == null || tags.length == 0) {
            return mockComponent(typeToMock);
        } else {
            mocks.add(new NodeMock(new NodeClassCandidate(typeToMock, tags)));
            return this;
        }
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (!(o instanceof KoraGraphModification that)) return false;
        return Objects.equals(additions, that.additions) && Objects.equals(replacements, that.replacements) && Objects.equals(mocks, that.mocks);
    }

    @Override
    public int hashCode() {
        return Objects.hash(additions, replacements, mocks);
    }
}
