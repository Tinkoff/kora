package ru.tinkoff.kora.validation.annotation.processor.testdata;

import com.typesafe.config.Config;
import com.typesafe.config.ConfigFactory;
import ru.tinkoff.kora.common.KoraApp;
import ru.tinkoff.kora.config.common.ConfigModule;
import ru.tinkoff.kora.validation.Validator;
import ru.tinkoff.kora.validation.constraint.ValidationModule;

@KoraApp
public interface AppWithConfig extends ConfigModule, ValidationModule {

    @Override
    default Config config() {
        return ConfigFactory.parseString(
            """
                """
        ).resolve();
    }

    default ValidationLifecycle lifecycle(Validator<Baby> babyValidator, Validator<Yoda> yodaValidator) {
        return new ValidationLifecycle(babyValidator, yodaValidator);
    }
}
