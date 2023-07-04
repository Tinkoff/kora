package ru.tinkoff.kora.test.extension.junit5.config;

import com.typesafe.config.Config;
import org.jetbrains.annotations.NotNull;
import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.Test;
import ru.tinkoff.kora.test.extension.junit5.KoraAppTest;
import ru.tinkoff.kora.test.extension.junit5.KoraAppTestConfigModifier;
import ru.tinkoff.kora.test.extension.junit5.KoraConfigModification;
import ru.tinkoff.kora.test.extension.junit5.TestComponent;
import ru.tinkoff.kora.test.extension.junit5.testdata.TestApplication;
import ru.tinkoff.kora.test.extension.junit5.testdata.TestComponent1;

import static org.junit.jupiter.api.Assertions.assertEquals;

@KoraAppTest(
    value = TestApplication.class,
    components = {TestComponent1.class})
public class ConfigMethodFileJUnitExtensionTests implements KoraAppTestConfigModifier {

    @Override
    public @NotNull KoraConfigModification config() {
        return KoraConfigModification.ofConfigFile("reference-raw.conf");
    }

    @Test
    void parameterConfigFromMethodInjected(@TestComponent Config config) {
        assertEquals("Config(SimpleConfigObject({\"myconfig\":{\"myinnerconfig\":{\"third\":3}}}))", config.toString());
    }
}
