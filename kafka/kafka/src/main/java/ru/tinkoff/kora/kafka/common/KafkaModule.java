package ru.tinkoff.kora.kafka.common;

import ru.tinkoff.kora.common.DefaultComponent;
import ru.tinkoff.kora.kafka.common.producer.telemetry.DefaultKafkaProducerTelemetryFactory;
import ru.tinkoff.kora.kafka.common.producer.telemetry.KafkaProducerLoggerFactory;
import ru.tinkoff.kora.kafka.common.producer.telemetry.KafkaProducerMetricsFactory;
import ru.tinkoff.kora.kafka.common.producer.telemetry.KafkaProducerTracerFactory;
import ru.tinkoff.kora.kafka.common.consumer.telemetry.DefaultKafkaConsumerTelemetry;
import ru.tinkoff.kora.kafka.common.consumer.telemetry.KafkaConsumerLogger;
import ru.tinkoff.kora.kafka.common.consumer.telemetry.KafkaConsumerMetrics;
import ru.tinkoff.kora.kafka.common.consumer.telemetry.KafkaConsumerTracer;

import javax.annotation.Nullable;

public interface KafkaModule extends KafkaDeserializersModule, KafkaSerializersModule {
    @DefaultComponent
    default <K, V> DefaultKafkaConsumerTelemetry<K, V> defaultKafkaConsumerTelemetry(@Nullable KafkaConsumerLogger<K, V> logger, @Nullable KafkaConsumerTracer tracing, @Nullable KafkaConsumerMetrics metrics) {
        return new DefaultKafkaConsumerTelemetry<>(logger, tracing, metrics);
    }

    @DefaultComponent
    default DefaultKafkaProducerTelemetryFactory defaultKafkaTelemetryFactory(@Nullable KafkaProducerTracerFactory tracer, @Nullable KafkaProducerLoggerFactory logger, @Nullable KafkaProducerMetricsFactory metrics) {
        return new DefaultKafkaProducerTelemetryFactory(tracer, logger, metrics);
    }
}
