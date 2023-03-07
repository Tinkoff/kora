package ru.tinkoff.kora.kafka.symbol.processor.consumer

import org.junit.jupiter.api.Test

class KafkaIncomingRecordTest : AbstractKafkaIncomingAnnotationProcessorTest() {
    @Test
    fun testProcessRecord() {
        compile("""
            class KafkaListener {
                @KafkaIncoming("test.config.path")
                fun process(event: ConsumerRecord<String, String>) {
                }
            }
            
            """.trimIndent())
    }

    @Test
    fun testProcessRecordAnyKeyType() {
        compile("""
            class KafkaListener {
                @KafkaIncoming("test.config.path")
                fun process(event: ConsumerRecord<*, String>) {
                }
            }
            
            """.trimIndent())
    }

    @Test
    fun testProcessRecordAndConsumer() {
        compile("""
            class KafkaListener {
                @KafkaIncoming("test.config.path")
                fun process(consumer: Consumer<String, String>, event: ConsumerRecord<String, String>) {
                }
            }
            
            """.trimIndent())
    }

    @Test
    fun testProcessRecordAndKeyParseException() {
        compile("""
            class KafkaListener {
                @KafkaIncoming("test.config.path")
                fun process(event: ConsumerRecord<String, String>?, exception: RecordKeyDeserializationException?) {
                }
            }
            
            """.trimIndent())
    }

    @Test
    fun testProcessRecordAndValueParseException() {
        compile("""
            class KafkaListener {
                @KafkaIncoming("test.config.path")
                fun process(event: ConsumerRecord<String, String>?, exception: RecordValueDeserializationException?) {
                }
            }
            
            """.trimIndent())
    }

    @Test
    fun testProcessRecordAndParseException() {
        compile("""
            class KafkaListener {
                @KafkaIncoming("test.config.path")
                fun process(event: ConsumerRecord<String, String>?, exception: Exception?) {
                }
            }
            
            """.trimIndent())
    }

    @Test
    fun testProcessRecordAndParseThrowable() {
        compile("""
            class KafkaListener {
                @KafkaIncoming("test.config.path")
                fun process(event: ConsumerRecord<String, String>?, exception: Throwable?) {
                }
            }
            
            """.trimIndent())
    }
}
