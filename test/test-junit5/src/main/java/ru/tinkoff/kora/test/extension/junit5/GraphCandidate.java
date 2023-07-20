package ru.tinkoff.kora.test.extension.junit5;

import ru.tinkoff.kora.application.graph.Node;

import javax.annotation.Nonnull;
import java.lang.reflect.Type;
import java.util.Arrays;
import java.util.Collections;
import java.util.List;

record GraphCandidate(@Nonnull Type type, @Nonnull List<Class<?>> tags) {

    GraphCandidate(Type type) {
        this(type, Collections.emptyList());
    }

    GraphCandidate(Type type, Class<?>[] tags) {
        this(type, (tags == null) ? Collections.emptyList() : Arrays.asList(tags));
    }

    public Class<?>[] tagsAsArray() {
        return tags.toArray(Class[]::new);
    }

    public boolean isSuitable(Node<?> node) {
        if (!node.type().equals(type)) {
            return false;
        }

        if (tags.isEmpty() && node.tags().length == 0) {
            return true;
        } else {
            return List.of(node.tags()).equals(tags);
        }
    }

    @Override
    public String toString() {
        return "[type=" + type + ", tags=" + tags + ']';
    }
}
