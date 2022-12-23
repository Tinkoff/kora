package ru.tinkoff.kora.test.extension.junit5;

import ru.tinkoff.kora.common.KoraApp;
import ru.tinkoff.kora.config.common.ConfigModule;
import ru.tinkoff.kora.http.client.jdk.JdkHttpClientModule;

@KoraApp
public interface HttpClientApplication extends ConfigModule, JdkHttpClientModule {

}
