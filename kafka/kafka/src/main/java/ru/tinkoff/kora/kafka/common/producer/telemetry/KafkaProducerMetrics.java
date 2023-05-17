package ru.tinkoff.kora.kafka.common.producer.telemetry;

public interface KafkaProducerMetrics extends AutoCloseable {
    @Override
    void close();
}
