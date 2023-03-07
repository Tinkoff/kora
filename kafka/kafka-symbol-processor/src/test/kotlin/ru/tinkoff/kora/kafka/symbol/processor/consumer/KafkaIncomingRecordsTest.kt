package ru.tinkoff.kora.kafka.symbol.processor.consumer

import org.junit.jupiter.api.Test

class KafkaIncomingRecordsTest : AbstractKafkaIncomingAnnotationProcessorTest() {
    @Test
    fun testProcessRecords() {
        compile("""
            class KafkaListener {
                @KafkaIncoming("test.config.path")
                fun process(event: ConsumerRecords<ByteArray, String>) {
                }
            }
            
            """.trimIndent())
    }

    @Test
    fun testProcessRecordsAnyKeyType() {
        compile("""
            class KafkaListener {
                @KafkaIncoming("test.config.path")
                fun process(event: ConsumerRecords<*, String>) {
                }
            }
            
            """.trimIndent())
    }

    @Test
    fun testProcessRecordsAndConsumer() {
        compile("""
            class KafkaListener {
                @KafkaIncoming("test.config.path")
                fun process(consumer: Consumer<*, *>, event: ConsumerRecords<String, String>) {
                }
            }
            
            """.trimIndent())
    }

    @Test
    fun testProcessRecordsAndConsumerAndTelemetry() {
        compile("""
            class KafkaListener {
                @KafkaIncoming("test.config.path")
                fun process(consumer: Consumer<*, *>, event: ConsumerRecords<ByteArray, String>, telemetry: KafkaConsumerTelemetry.KafkaConsumerRecordsTelemetryContext<*, *>) {
                }
            }
            
            """.trimIndent())
    }

    @Test
    fun testProcessRecordsAndTelemetry() {
        compile("""
            class KafkaListener {
                @KafkaIncoming("test.config.path")
                fun process(telemetry: KafkaConsumerTelemetry.KafkaConsumerRecordsTelemetryContext<*, *>, event: ConsumerRecords<ByteArray, String>) {
                }
            }
            
            """.trimIndent())
    }
}
