package ru.tinkoff.kora.test.extension.junit5;

import org.jetbrains.annotations.NotNull;

import java.util.ArrayList;
import java.util.List;
import java.util.function.Function;
import java.util.function.Supplier;

public class KoraGraphModifier {

    public record NodeAddition(Function<KoraAppTestGraph, ?> function, Class<?>[] tags) {}

    public record NodeReplacement(Function<KoraAppTestGraph, ?> function, NodeToReplace replacement) {

        public record NodeToReplace(Class<?> type, Class<?>[] tags) {}
    }

    private final List<NodeAddition> additions = new ArrayList<>();
    private final List<NodeReplacement> replacements = new ArrayList<>();

    List<NodeAddition> getAdditions() {
        return additions;
    }

    List<NodeReplacement> getReplacements() {
        return replacements;
    }

    public <T> KoraGraphModifier addNode(@NotNull Supplier<T> instanceSupplier) {
        additions.add(new NodeAddition(s -> instanceSupplier.get(), null));
        return this;
    }

    public <T> KoraGraphModifier addNode(@NotNull Supplier<T> instanceSupplier,
                                         Class<?>... tags) {
        additions.add(new NodeAddition(s -> instanceSupplier.get(), tags));
        return this;
    }

    public <T> KoraGraphModifier addNode(@NotNull Function<KoraAppTestGraph, T> instanceSupplier) {
        additions.add(new NodeAddition(instanceSupplier, null));
        return this;
    }

    public <T> KoraGraphModifier addNode(@NotNull Function<KoraAppTestGraph, T> instanceSupplier,
                                         Class<?>... tags) {
        additions.add(new NodeAddition(instanceSupplier, tags));
        return this;
    }

    public <T> KoraGraphModifier replaceNode(@NotNull Supplier<? extends T> replacementSupplier,
                                             @NotNull Class<? extends T> typeToReplace) {
        replacements.add(new NodeReplacement(g -> replacementSupplier.get(), new NodeReplacement.NodeToReplace(typeToReplace, null)));
        return this;
    }

    public <T> KoraGraphModifier replaceNode(@NotNull Supplier<? extends T> replacementSupplier,
                                             @NotNull Class<? extends T> typeToReplace,
                                             Class<?>... tags) {
        replacements.add(new NodeReplacement(g -> replacementSupplier.get(), new NodeReplacement.NodeToReplace(typeToReplace, tags)));
        return this;
    }

    public <T> KoraGraphModifier replaceNode(@NotNull Function<KoraAppTestGraph, ? extends T> replacementSupplier,
                                             @NotNull Class<? extends T> typeToReplace) {
        replacements.add(new NodeReplacement(replacementSupplier, new NodeReplacement.NodeToReplace(typeToReplace, null)));
        return this;
    }

    public <T> KoraGraphModifier replaceNode(@NotNull Function<KoraAppTestGraph, ? extends T> replacementSupplier,
                                             @NotNull Class<? extends T> typeToReplace,
                                             Class<?>... tags) {
        replacements.add(new NodeReplacement(replacementSupplier, new NodeReplacement.NodeToReplace(typeToReplace, tags)));
        return this;
    }
}
