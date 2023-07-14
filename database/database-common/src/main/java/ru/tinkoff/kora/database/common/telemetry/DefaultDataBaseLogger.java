package ru.tinkoff.kora.database.common.telemetry;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import ru.tinkoff.kora.database.common.QueryContext;
import ru.tinkoff.kora.logging.common.arg.StructuredArgument;

import javax.annotation.Nullable;

public class DefaultDataBaseLogger implements DataBaseLogger {

    private final Logger log;
    private final String poolName;
    private final String dbType;
    private final String driverType;

    public DefaultDataBaseLogger(String poolName, String dbType, String driverType) {
        this.poolName = poolName;
        this.dbType = dbType;
        this.driverType = driverType;
        this.log = LoggerFactory.getLogger("ru.tinkoff.kora.database." + driverType + "." + dbType + "." + poolName + ".query");
    }

    @Override
    public boolean isEnabled() {
        return this.log.isInfoEnabled();
    }

    @Override
    public void logQueryBegin(QueryContext queryContext) {
        var marker = StructuredArgument.marker("sqlQuery", gen -> {
            gen.writeStartObject();
            gen.writeStringField("pool", this.poolName);
            gen.writeStringField("database", this.dbType);
            gen.writeStringField("driver", this.driverType);
            gen.writeStringField("queryId", queryContext.queryId());
            gen.writeEndObject();
        });

        if (log.isDebugEnabled()) {
            log.debug(marker, "SQL executing for pool '{}':\n{}",this.poolName, queryContext.sql());
        } else if (log.isInfoEnabled()) {
            log.info(marker, "SQL executing for pool '{}'", this.poolName);
        }
    }

    @Override
    public void logQueryEnd(long duration, QueryContext queryContext, @Nullable Throwable ex) {
        var marker = StructuredArgument.marker("sqlQuery", gen -> {
            gen.writeStartObject();
            gen.writeStringField("pool", this.poolName);
            gen.writeStringField("database", this.dbType);
            gen.writeStringField("driver", this.driverType);
            gen.writeStringField("queryId", queryContext.queryId());
            gen.writeNumberField("processingTime", duration / 1_000_000);
            gen.writeEndObject();
        });

        if (log.isDebugEnabled()) {
            log.debug(marker, "SQL executed for pool '{}':\n{}", this.poolName, queryContext.sql());
        } else if(log.isInfoEnabled()) {
            log.info(marker, "SQL executed for pool '{}'", this.poolName);
        }
    }
}
