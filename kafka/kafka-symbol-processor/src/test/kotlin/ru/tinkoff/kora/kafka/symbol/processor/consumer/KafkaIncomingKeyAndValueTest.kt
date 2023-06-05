package ru.tinkoff.kora.kafka.symbol.processor.consumer

import org.apache.kafka.common.serialization.Deserializer
import org.assertj.core.api.Assertions.assertThat
import org.junit.jupiter.api.Test
import ru.tinkoff.kora.common.Tag
import ru.tinkoff.kora.kafka.common.config.KafkaConsumerConfig
import ru.tinkoff.kora.kafka.common.consumer.containers.handlers.KafkaRecordHandler
import ru.tinkoff.kora.kafka.common.consumer.telemetry.KafkaConsumerTelemetry

class KafkaIncomingKeyAndValueTest : AbstractKafkaIncomingAnnotationProcessorTest() {
    @Test
    fun testProcessValue() {
        compile("""
            class KafkaListener {
                @KafkaIncoming("test.config.path")
                fun process(value: String) {
                }
            }
            
            """.trimIndent())
    }

    @Test
    fun testProcessValueWithTag() {
        compile("""
            class KafkaListener {
                @KafkaIncoming("test.config.path")
                fun process(@Tag(KafkaListener::class) value: String) {
                }
            }
            
            """.trimIndent())
        compileResult.assertSuccess()
        val module = compileResult.loadClass("KafkaListenerModule")
        val container = module.getMethod("kafkaListenerProcessContainer", KafkaConsumerConfig::class.java, KafkaRecordHandler::class.java, Deserializer::class.java, Deserializer::class.java, KafkaConsumerTelemetry::class.java)
        val valueDeserializer = container.parameters[3]

        val valueTag = valueDeserializer.getAnnotation(Tag::class.java)

        assertThat(valueTag).isNotNull()
        assertThat(valueTag.value.map { it.java }).isEqualTo(listOf(compileResult.loadClass("KafkaListener")))
    }

    @Test
    fun testProcessKeyAndValue() {
        compile("""
            class KafkaListener {
                @KafkaIncoming("test.config.path")
                fun process(key: String, value: String) {
                }
            }
            
            """.trimIndent())
    }

    @Test
    fun testProcessKeyAndValueWithTag() {
        compile("""
            class KafkaListener {
                @KafkaIncoming("test.config.path")
                fun process(@Tag(KafkaListener::class) key: String, @Tag(String::class) value: String) {
                }
            }
            
            """.trimIndent())
        val module = compileResult.loadClass("KafkaListenerModule")
        val container = module.getMethod("kafkaListenerProcessContainer", KafkaConsumerConfig::class.java, KafkaRecordHandler::class.java, Deserializer::class.java, Deserializer::class.java, KafkaConsumerTelemetry::class.java)
        val keyDeserializer = container.parameters[2]
        val valueDeserializer = container.parameters[3]

        val keyTag = keyDeserializer.getAnnotation(Tag::class.java)
        val valueTag = valueDeserializer.getAnnotation(Tag::class.java)

        assertThat(keyTag).isNotNull()
        assertThat(keyTag.value.map { it.java }).isEqualTo(listOf(compileResult.loadClass("KafkaListener")))
        assertThat(valueTag).isNotNull()
        assertThat(valueTag.value.map { it.java }).isEqualTo(listOf(String::class.java))
    }

    @Test
    fun testProcessValueAndValueException() {
        compile("""
            class KafkaListener {
                @KafkaIncoming("test.config.path")
                fun process(value: String?, exception: RecordValueDeserializationException?) {
                }
            }
            
            """.trimIndent())
    }

    @Test
    fun testProcessValueAndException() {
        compile("""
            class KafkaListener {
                @KafkaIncoming("test.config.path")
                fun process(value: String?, exception: Exception?) {
                }
            }
            
            """.trimIndent())
    }

    @Test
    fun testProcessValueAndConsumer() {
        compile("""
            class KafkaListener {
                @KafkaIncoming("test.config.path")
                fun process(consumer: Consumer<*,*>, value: String?, exception: Exception?) {
                }
            }
            
            """.trimIndent())
    }
}
