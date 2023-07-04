package ru.tinkoff.kora.test.extension.junit5;

import java.lang.reflect.Type;
import java.util.Arrays;
import java.util.List;

record GraphCandidate(Type type, Class<?>[] tags) {

    GraphCandidate(Type type) {
        this(type, (Class<?>[]) null);
    }

    GraphCandidate(Type type, List<Class<?>> tags) {
        this(type, tags.toArray(Class<?>[]::new));
    }

    @Override
    public String toString() {
        return "[type=" + type + ", tags=" + Arrays.toString(tags) + ']';
    }
}
