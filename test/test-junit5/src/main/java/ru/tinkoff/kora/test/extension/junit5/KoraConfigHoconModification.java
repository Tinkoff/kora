package ru.tinkoff.kora.test.extension.junit5;

import com.typesafe.config.Config;
import com.typesafe.config.ConfigFactory;
import com.typesafe.config.ConfigParseOptions;
import com.typesafe.config.ConfigRenderOptions;
import org.intellij.lang.annotations.Language;

import javax.annotation.Nonnull;
import java.util.HashMap;
import java.util.Map;

/**
 * Configs inside {@link KoraConfigHoconModification} are merged in user specified method order
 * <p>
 * All configs specified for test are merged into single config, each next config replaces values from previous configs
 */
public final class KoraConfigHoconModification implements KoraConfigModification {

    private final Map<String, String> systemProperties = new HashMap<>();
    private Config mergedConfig;

    KoraConfigHoconModification(Config config) {
        this.mergedConfig = config;
    }

    @Nonnull
    @Override
    public String config() {
        return mergedConfig
            .root()
            .render(ConfigRenderOptions.concise().setFormatted(true));
    }

    @Nonnull
    @Override
    public Map<String, String> systemProperties() {
        return Map.copyOf(systemProperties);
    }

    /**
     * @param config application configuration in HOCON format as string
     * @return self
     */
    @Nonnull
    public KoraConfigHoconModification mergeWithConfigHocon(@Nonnull @Language("HOCON") String config) {
        for (var entry : configOfHOCON(config).entrySet()) {
            this.mergedConfig = this.mergedConfig.withValue(entry.getKey(), entry.getValue());
        }
        return this;
    }

    /**
     * @param configFile application configuration in HOCON format as file from Resources
     * @return self
     */
    @Nonnull
    public KoraConfigHoconModification mergeWithConfigHoconFile(@Nonnull String configFile) {
        for (var entry : configOfResource(configFile).entrySet()) {
            this.mergedConfig = this.mergedConfig.withValue(entry.getKey(), entry.getValue());
        }
        return this;
    }

    @Nonnull
    public KoraConfigHoconModification withSystemProperty(@Nonnull String key, @Nonnull String value) {
        this.systemProperties.put(key, value);
        return this;
    }

    @Nonnull
    static Config configOfHOCON(@Language("HOCON") String config) {
        return ConfigFactory.parseString(config, ConfigParseOptions.defaults().setAllowMissing(true));
    }

    @Nonnull
    static Config configOfResource(String configFile) {
        return ConfigFactory.parseResources(configFile, ConfigParseOptions.defaults().setAllowMissing(true));
    }

    @Nonnull
    Map<String, String> getSystemProperties() {
        return systemProperties;
    }

    @Override
    public String toString() {
        return config();
    }
}
