package ru.tinkoff.kora.example;

import ru.tinkoff.kora.common.KoraApp;
import ru.tinkoff.kora.config.common.ConfigModule;
import ru.tinkoff.kora.http.client.jdk.JdkHttpClientModule;

@KoraApp
public interface TestHttpClientApplication extends ConfigModule, JdkHttpClientModule {

}
