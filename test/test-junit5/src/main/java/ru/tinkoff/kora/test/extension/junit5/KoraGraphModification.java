package ru.tinkoff.kora.test.extension.junit5;

import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import java.lang.reflect.Type;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.util.Objects;
import java.util.function.Function;
import java.util.function.Supplier;

public final class KoraGraphModification {

    record NodeAddition(Function<KoraAppGraph, ?> function, NodeTypeCandidate candidate) {}

    record NodeReplacement(Function<KoraAppGraph, ?> function, NodeTypeCandidate candidate) {}

    record NodeMock(NodeTypeCandidate candidate) {}

    record NodeComponentCandidate(Class<?> type, Class<?>[] tags) {

        NodeComponentCandidate(Class<?> type) {
            this(type, (Class<?>[]) null);
        }

        NodeComponentCandidate(Class<?> type, List<Class<?>> tags) {
            this(type, tags.toArray(Class<?>[]::new));
        }

        @Override
        public String toString() {
            return "[type=" + type + ", tags=" + Arrays.toString(tags) + ']';
        }
    }

    record NodeTypeCandidate(Type type, Class<?>[] tags) {

        NodeTypeCandidate(Type type) {
            this(type, (Class<?>[]) null);
        }

        NodeTypeCandidate(Type type, List<Class<?>> tags) {
            this(type, tags.toArray(Class<?>[]::new));
        }

        @Override
        public String toString() {
            return "[type=" + type + ", tags=" + Arrays.toString(tags) + ']';
        }
    }

    private final List<NodeAddition> additions = new ArrayList<>();
    private final List<NodeReplacement> replacements = new ArrayList<>();
    private final List<NodeMock> mocks = new ArrayList<>();

    private KoraGraphModification() {}

    public static KoraGraphModification of() {
        return new KoraGraphModification();
    }

    /**
     * Component that should be added to Graph Context
     */
    public <T> KoraGraphModification addComponent(@NotNull Type typeToAdd,
                                                  @NotNull Supplier<T> instanceSupplier) {
        additions.add(new NodeAddition(s -> instanceSupplier.get(), new NodeTypeCandidate(typeToAdd)));
        return this;
    }

    /**
     * Component that should be added to Graph Context
     */
    public <T> KoraGraphModification addComponent(@NotNull Type typeToAdd,
                                                  @NotNull List<Class<?>> tags,
                                                  @NotNull Supplier<T> instanceSupplier) {
        if (tags.isEmpty()) {
            return addComponent(typeToAdd, instanceSupplier);
        } else {
            additions.add(new NodeAddition(s -> instanceSupplier.get(), new NodeTypeCandidate(typeToAdd, tags)));
            return this;
        }
    }

    /**
     * Component that should be added to Graph Context
     */
    public <T> KoraGraphModification addComponent(@NotNull Type typeToAdd,
                                                  @NotNull Function<KoraAppGraph, T> instanceSupplier) {
        additions.add(new NodeAddition(instanceSupplier, new NodeTypeCandidate(typeToAdd)));
        return this;
    }

    /**
     * Component that should be added to Graph Context
     */
    public <T> KoraGraphModification addComponent(@NotNull Type typeToAdd,
                                                  @NotNull List<Class<?>> tags,
                                                  @NotNull Function<KoraAppGraph, T> instanceSupplier) {
        if (tags.isEmpty()) {
            return addComponent(typeToAdd, instanceSupplier);
        } else {
            additions.add(new NodeAddition(instanceSupplier, new NodeTypeCandidate(typeToAdd, tags)));
            return this;
        }
    }

    /**
     * Component that should replace existing one with new one
     */
    public <T> KoraGraphModification replaceComponent(@NotNull Type typeToReplace,
                                                      @NotNull Supplier<? extends T> replacementSupplier) {
        replacements.add(new NodeReplacement(g -> replacementSupplier.get(), new NodeTypeCandidate(typeToReplace)));
        return this;
    }

    /**
     * Component that should replace existing one with new one
     */
    public <T> KoraGraphModification replaceComponent(@NotNull Type typeToReplace,
                                                      @NotNull List<Class<?>> tags,
                                                      @NotNull Supplier<? extends T> replacementSupplier) {
        if (tags.isEmpty()) {
            return replaceComponent(typeToReplace, replacementSupplier);
        } else {
            replacements.add(new NodeReplacement(g -> replacementSupplier.get(), new NodeTypeCandidate(typeToReplace, tags)));
            return this;
        }
    }

    /**
     * Component that should replace existing one with new one
     */
    public <T> KoraGraphModification replaceComponent(@NotNull Type typeToReplace,
                                                      @NotNull Function<KoraAppGraph, ? extends T> replacementSupplier) {
        replacements.add(new NodeReplacement(replacementSupplier, new NodeTypeCandidate(typeToReplace)));
        return this;
    }

    /**
     * Component that should replace existing one with new one
     */
    public <T> KoraGraphModification replaceComponent(@NotNull Type typeToReplace,
                                                      @NotNull List<Class<?>> tags,
                                                      @NotNull Function<KoraAppGraph, ? extends T> replacementSupplier) {
        if (tags.isEmpty()) {
            return replaceComponent(typeToReplace, replacementSupplier);
        } else {
            replacements.add(new NodeReplacement(replacementSupplier, new NodeTypeCandidate(typeToReplace, tags)));
            return this;
        }
    }

    /**
     * Component that should be Mocked with {@link org.mockito.Mockito}
     */
    public KoraGraphModification mockComponent(@NotNull Type typeToMock) {
        mocks.add(new NodeMock(new NodeTypeCandidate(typeToMock)));
        return this;
    }

    /**
     * Component that should be Mocked with {@link org.mockito.Mockito}
     */
    public KoraGraphModification mockComponent(@NotNull Type typeToMock,
                                               @NotNull List<Class<?>> tags) {
        if (tags.isEmpty()) {
            return mockComponent(typeToMock);
        } else {
            mocks.add(new NodeMock(new NodeTypeCandidate(typeToMock, tags)));
            return this;
        }
    }

    /**
     * Component that should be Mocked with {@link org.mockito.Mockito}
     */
    KoraGraphModification mockComponent(@NotNull Type typeToMock, @Nullable Class<?>[] tags) {
        if (tags == null) {
            return mockComponent(typeToMock);
        } else {
            mocks.add(new NodeMock(new NodeTypeCandidate(typeToMock, tags)));
            return this;
        }
    }

    List<NodeAddition> getAdditions() {
        return additions;
    }

    List<NodeReplacement> getReplacements() {
        return replacements;
    }

    List<NodeMock> getMocks() {
        return mocks;
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
