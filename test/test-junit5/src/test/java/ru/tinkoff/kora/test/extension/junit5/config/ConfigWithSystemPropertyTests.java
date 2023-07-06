package ru.tinkoff.kora.test.extension.junit5.config;

import com.typesafe.config.Config;
import org.jetbrains.annotations.NotNull;
import org.junit.jupiter.api.Test;
import ru.tinkoff.kora.test.extension.junit5.KoraAppTest;
import ru.tinkoff.kora.test.extension.junit5.KoraAppTestConfigModifier;
import ru.tinkoff.kora.test.extension.junit5.KoraConfigModification;
import ru.tinkoff.kora.test.extension.junit5.TestComponent;
import ru.tinkoff.kora.test.extension.junit5.testdata.TestConfigApplication;

import static org.junit.jupiter.api.Assertions.assertEquals;

@KoraAppTest(TestConfigApplication.class)
public class ConfigWithSystemPropertyTests implements KoraAppTestConfigModifier {

    @Override
    public @NotNull KoraConfigModification config() {
        return KoraConfigModification.ofSystemProperty("one", "1")
            .withSystemProperty("two", "2");
    }

    @Test
    void parameterConfigFromMethodInjected(@TestComponent Config config) {
        assertEquals(1L, config.getNumber("one"));
        assertEquals(2L, config.getNumber("two"));
    }
}
