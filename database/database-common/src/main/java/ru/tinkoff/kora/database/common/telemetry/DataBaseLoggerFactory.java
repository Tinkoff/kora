package ru.tinkoff.kora.database.common.telemetry;

public interface DataBaseLoggerFactory {

    DataBaseLogger get(String poolName, String dbType, String driverType);

    final class DefaultDataBaseLoggerFactory implements DataBaseLoggerFactory {
        @Override
        public DataBaseLogger get(String poolName, String dbType, String driverType) {
            return new DefaultDataBaseLogger(poolName, dbType, driverType);
        }
    }
}
