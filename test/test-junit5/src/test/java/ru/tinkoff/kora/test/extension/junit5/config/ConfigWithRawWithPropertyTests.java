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
import static org.junit.jupiter.api.Assertions.assertNotNull;

@KoraAppTest(TestConfigApplication.class)
public class ConfigWithRawWithPropertyTests implements KoraAppTestConfigModifier {

    @Override
    public @NotNull KoraConfigModification config() {
        return KoraConfigModification.ofString("""
                myconfig {
                  myinnerconfig {
                    second = ${ENV_SECOND}
                  }
                }
                """)
            .withSystemProperty("ENV_SECOND", "value");
    }

    @Test
    void parameterConfigFromMethodInjected(@TestComponent Config config) {
        assertNotNull(config.getObject("myconfig"));
        assertNotNull(config.getObject("myconfig.myinnerconfig"));
        assertEquals("value", config.getString("myconfig.myinnerconfig.second"));
    }
}
