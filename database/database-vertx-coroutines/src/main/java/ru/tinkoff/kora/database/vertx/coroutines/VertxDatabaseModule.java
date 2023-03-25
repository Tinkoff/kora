package ru.tinkoff.kora.database.vertx.coroutines;

import com.typesafe.config.Config;
import io.netty.channel.EventLoopGroup;
import ru.tinkoff.kora.config.common.extractor.ConfigValueExtractor;
import ru.tinkoff.kora.database.common.telemetry.DataBaseTelemetryFactory;
import ru.tinkoff.kora.database.vertx.VertxDatabaseBaseModule;
import ru.tinkoff.kora.database.vertx.VertxDatabaseConfig;

public interface VertxDatabaseModule extends VertxDatabaseBaseModule {
    default VertxDatabaseConfig vertxDatabaseConfig(Config config, ConfigValueExtractor<VertxDatabaseConfig> extractor) {
        var value = config.getValue("db");
        return extractor.extract(value);
    }

    default VertxDatabase vertxDatabase(VertxDatabaseConfig vertxDatabaseConfig, EventLoopGroup eventLoopGroup, DataBaseTelemetryFactory telemetryFactory) {
        return new VertxDatabase(vertxDatabaseConfig, eventLoopGroup, telemetryFactory);
    }
}
