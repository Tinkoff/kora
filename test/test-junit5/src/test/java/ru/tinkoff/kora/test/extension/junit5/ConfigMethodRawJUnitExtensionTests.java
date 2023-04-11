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
public class ConfigMethodRawJUnitExtensionTests extends Assertions implements KoraAppTestConfig {

    @Override
    public @NotNull KoraConfigModification config() {
        return KoraConfigModification.ofConfigHocon("""
            myconfig {
              myinnerconfig {
                myproperty = 1
              }
            }
            """);
    }

    @Test
    void parameterConfigFromMethodInjected(@TestComponent Config config) {
        assertEquals("Config(SimpleConfigObject({\"myconfig\":{\"myinnerconfig\":{\"myproperty\":1}}}))", config.toString());
    }
}
