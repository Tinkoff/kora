package ru.tinkoff.kora.validation.annotation.processor.testdata;

import com.typesafe.config.Config;
import com.typesafe.config.ConfigFactory;
import ru.tinkoff.kora.common.KoraApp;
import ru.tinkoff.kora.config.common.ConfigModule;
import ru.tinkoff.kora.validation.constraint.ValidationModule;
import ru.tinkoff.kora.validation.constraint.factory.NotEmptyConstraintFactory;
import ru.tinkoff.kora.validation.constraint.factory.NotNullConstraintFactory;
import ru.tinkoff.kora.validation.constraint.factory.RangeConstraintFactory;

import java.util.List;

@KoraApp
public interface AppWithConfig extends ConfigModule, ValidationModule {

    @Override
    default Config config() {
        return ConfigFactory.parseString(
            """
                """
        ).resolve();
    }

    default BabyValidator babyValidator(NotNullConstraintFactory<String> constraintFactory1,
                                        NotEmptyConstraintFactory<String> constraintFactory2,
                                        RangeConstraintFactory<Long> constraintFactory3) {
        return new BabyValidator(constraintFactory1, constraintFactory2, constraintFactory3);
    }

    default YodaValidator yodaValidator(NotNullConstraintFactory<String> constraintFactory1,
                                        NotEmptyConstraintFactory<String> constraintFactory2,
                                        NotEmptyConstraintFactory<List<Baby>> constraintFactory3) {
        return new YodaValidator(constraintFactory1, constraintFactory2, constraintFactory3);
    }

    default ValidationLifecycle lifecycle(BabyValidator babyValidator, YodaValidator yodaValidator) {
        return new ValidationLifecycle(babyValidator, yodaValidator);
    }
}
