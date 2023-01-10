package ru.tinkoff.kora.test.extension.junit5;

import org.intellij.lang.annotations.Language;

public interface KoraAppTestConfigProvider {

    /**
     * @return analog to {@link KoraAppTest#config()}, but is useful when some part of configuration is dynamic (like TestContainer container port)
     */
    @Language("HOCON")
    String config();
}
