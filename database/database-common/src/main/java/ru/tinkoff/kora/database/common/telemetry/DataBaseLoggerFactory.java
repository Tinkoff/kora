package ru.tinkoff.kora.database.common.telemetry;

public interface DataBaseLoggerFactory {

    DataBaseLogger get(String driverType, String poolName);

    final class DefaultDataBaseLoggerFactory implements DataBaseLoggerFactory {
        @Override
        public DataBaseLogger get(String driverType, String poolName) {
            return new DefaultDataBaseLogger(driverType, poolName);
        }
    }
}
