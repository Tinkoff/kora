package ru.tinkoff.kora.kafka

import ru.tinkoff.kora.common.DefaultComponent
import ru.tinkoff.kora.kafka.common.KafkaDeserializersModule
import ru.tinkoff.kora.kafka.common.KafkaSerializersModule
import ru.tinkoff.kora.kafka.common.consumer.telemetry.DefaultKafkaConsumerTelemetry
import ru.tinkoff.kora.kafka.common.consumer.telemetry.KafkaConsumerLogger
import ru.tinkoff.kora.kafka.common.consumer.telemetry.KafkaConsumerMetrics
import ru.tinkoff.kora.kafka.common.consumer.telemetry.KafkaConsumerTracer

interface KafkaKotlinModule : KafkaDeserializersModule, KafkaSerializersModule {
    @DefaultComponent
    fun <K, V> defaultKafkaConsumerTelemetry(
        logger: KafkaConsumerLogger<K, V>?,
        tracing: KafkaConsumerTracer?,
        metrics: KafkaConsumerMetrics?
    ): DefaultKafkaConsumerTelemetry<K, V> {
        return DefaultKafkaConsumerTelemetry(logger, tracing, metrics)
    }
}
