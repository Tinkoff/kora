package ru.tinkoff.kora.common;

import javax.annotation.Nullable;

public interface Principal {
    Context.Key<Principal> KEY = new Context.KeyImmutable<>() {};

    @Nullable
    static Principal current() {
        return current(Context.current());
    }

    @Nullable
    static Principal current(Context context) {
        return context.get(KEY);
    }

    @Nullable
    static Principal set(Context context, Principal principal) {
        return context.set(KEY, principal);
    }
}
