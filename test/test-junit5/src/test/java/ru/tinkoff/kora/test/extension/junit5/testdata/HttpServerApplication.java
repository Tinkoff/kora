package ru.tinkoff.kora.test.extension.junit5.testdata;

import ru.tinkoff.kora.common.KoraApp;
import ru.tinkoff.kora.http.server.undertow.UndertowHttpServerModule;

@KoraApp
public interface HttpServerApplication extends UndertowHttpServerModule {

}
