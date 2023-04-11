package ru.tinkoff.kora.test.extension.junit5;

import com.typesafe.config.Config;
import com.typesafe.config.ConfigFactory;
import com.typesafe.config.ConfigParseOptions;
import com.typesafe.config.ConfigRenderOptions;
import org.intellij.lang.annotations.Language;

import javax.annotation.Nonnull;

/**
 * Configs inside {@link KoraConfigModification} are merged in user specified method order
 * <p>
 * All configs specified for test are merged into single config, each next config replaces values from previous configs
 */
public final class KoraConfigModification {

    private Config mergedConfig;

    private KoraConfigModification(Config config) {
        this.mergedConfig = config;
    }

    /**
     * @param config application configuration in HOCON format as string
     * @return self
     */
    public static KoraConfigModification ofConfigHocon(@Nonnull @Language("HOCON") String config) {
        return new KoraConfigModification(configOfHOCON(config));
    }

    /**
     * @param configFile application configuration in HOCON format as file from Resources
     * @return self
     */
    public static KoraConfigModification ofConfigFile(@Nonnull String configFile) {
        return new KoraConfigModification(configOfResource(configFile));
    }

    /**
     * @param config application configuration in HOCON format as string
     * @return self
     */
    public KoraConfigModification mergeWithConfigHocon(@Nonnull @Language("HOCON") String config) {
        for (var entry : configOfHOCON(config).entrySet()) {
            this.mergedConfig = this.mergedConfig.withValue(entry.getKey(), entry.getValue());
        }
        return this;
    }

    /**
     * @param configFile application configuration in HOCON format as file from Resources
     * @return self
     */
    public KoraConfigModification mergeWithConfigFile(@Nonnull String configFile) {
        for (var entry : configOfResource(configFile).entrySet()) {
            this.mergedConfig = this.mergedConfig.withValue(entry.getKey(), entry.getValue());
        }
        return this;
    }

    @Nonnull
    String getConfig() {
        return mergedConfig.resolve()
            .root()
            .render(ConfigRenderOptions.concise());
    }

    private static Config configOfHOCON(@Language("HOCON") String config) {
        return ConfigFactory.parseString(config, ConfigParseOptions.defaults().setAllowMissing(true));
    }

    private static Config configOfResource(String configFile) {
        return ConfigFactory.parseResources(configFile, ConfigParseOptions.defaults().setAllowMissing(true));
    }

    @Override
    public String toString() {
        return getConfig();
    }
}
