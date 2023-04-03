package ru.tinkoff.kora.test.extension.junit5;

import com.typesafe.config.Config;
import org.jetbrains.annotations.NotNull;
import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.Test;
import ru.tinkoff.kora.test.extension.junit5.testdata.Component1;
import ru.tinkoff.kora.test.extension.junit5.testdata.LifecycleApplication;

@KoraAppTest(
    application = LifecycleApplication.class,
    components = {Component1.class})
public class ConfigMethodFileWithRawWithFileAnnotationWithRawAnnotationJUnitExtensionTests extends Assertions implements KoraAppTestConfig {

    @Override
    public @NotNull KoraConfigModification config() {
        return KoraConfigModification.ofConfigFile("reference-raw.conf")   // 1
            .mergeWithConfig("""
                            myconfig {
                              myinnerconfig {
                                myproperty = 1
                                fourth = 4
                              }
                            }
                """);                                                       // 2
    }

    @Test
    void parameterConfigFromMethodInjected(@TestComponent Config config) {
        assertEquals("Config(SimpleConfigObject({\"myconfig\":{\"myinnerconfig\":{\"fourth\":4,\"myproperty\":1,\"third\":3}}}))", config.toString());
    }
}
