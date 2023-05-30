package ru.tinkoff.kora.test.extension.junit5;

import javax.annotation.Nonnull;

/**
 * Is useful when some part of configuration is dynamically configured (like TestContainer container port)
 */
public interface KoraAppTestConfigModifier {

    /**
     * @return Kora Config modification for {@link KoraAppTest} tests
     */
    @Nonnull
    KoraConfigModification config();
}
