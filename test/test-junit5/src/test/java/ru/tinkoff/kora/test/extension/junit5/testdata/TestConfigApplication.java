package ru.tinkoff.kora.test.extension.junit5.testdata;

import ru.tinkoff.kora.common.KoraApp;
import ru.tinkoff.kora.config.hocon.HoconConfigModule;

@KoraApp
public interface TestConfigApplication extends HoconConfigModule {

}
