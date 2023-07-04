package ru.tinkoff.kora.test.extension.junit5;

import org.intellij.lang.annotations.Language;

import javax.annotation.Nonnull;
import java.util.Map;

/**
 * Configs inside {@link KoraConfigModification} are merged in user specified method order
 * <p>
 * All configs specified for test are merged into single config, each next config replaces values from previous configs
 */
public interface KoraConfigModification {

    @Nonnull
    String config();

    @Nonnull
    Map<String, String> systemProperties();

    /**
     * @param config application configuration in HOCON format as string
     * @return self
     */
    @Nonnull
    static KoraConfigHoconModification ofConfigHocon(@Nonnull @Language("HOCON") String config) {
        return new KoraConfigHoconModification(KoraConfigHoconModification.configOfHOCON(config));
    }

    /**
     * @param configFile application configuration in HOCON format as file from Resources
     * @return self
     */
    @Nonnull
    static KoraConfigHoconModification ofConfigHoconFile(@Nonnull String configFile) {
        return new KoraConfigHoconModification(KoraConfigHoconModification.configOfResource(configFile));
    }
}
