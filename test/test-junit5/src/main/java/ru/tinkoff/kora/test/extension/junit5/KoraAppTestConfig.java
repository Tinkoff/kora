package ru.tinkoff.kora.test.extension.junit5;

import org.intellij.lang.annotations.Language;
import org.jetbrains.annotations.NotNull;

public interface KoraAppTestConfig {

    /**
     * @return analog to {@link KoraAppTest#config()}, but is useful when some part of configuration is dynamic (like TestContainer container port)
     */
    @Language("HOCON")
    @NotNull
    String config();
}
