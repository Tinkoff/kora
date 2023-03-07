package ru.tinkoff.kora.kafka.symbol.processor.consumer

import org.junit.jupiter.api.Test

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
