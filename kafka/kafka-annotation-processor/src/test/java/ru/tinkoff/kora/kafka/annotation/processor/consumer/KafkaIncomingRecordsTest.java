package ru.tinkoff.kora.kafka.annotation.processor.consumer;

import org.junit.jupiter.api.Test;

public class KafkaIncomingRecordsTest extends AbstractKafkaIncomingAnnotationProcessorTest {
    @Test
    public void testProcessRecords() {
        var handler = compile("""
            public class KafkaListener {
                @KafkaIncoming("test.config.path")
                public void process(ConsumerRecords<byte[], String> event) {
                }
            }
            """)
            .recordsHandler(byte[].class, String.class);

        handler.handle(record("test".getBytes(), "test-value"), i -> i
            .assertRecords(0)
            .hasSize(1)
            .hasRecord(0, v -> v
                .hasKey("test".getBytes())
                .hasValue("test-value")
            )
        );
        handler.handle(errorKey("test-value"), i -> i
            .assertRecords(0)
            .hasSize(1)
            .hasRecord(0, v -> v
                .hasKeyError()
                .hasValue("test-value")
            )
        );
        handler.handle(errorValue("test".getBytes()), i -> i
            .assertRecords(0)
            .hasSize(1)
            .hasRecord(0, v -> v
                .hasKey("test".getBytes())
                .hasValueError()
            )
        );
    }

    @Test
    public void testProcessRecordsAnyKeyType() {
        var handler = compile("""
            public class KafkaListener {
                @KafkaIncoming("test.config.path")
                public void process(ConsumerRecords<?, String> event) {
                }
            }
            """)
            .recordsHandler(byte[].class, String.class);

        handler.handle(record("test".getBytes(), "test-value"), i -> i
            .assertRecords(0)
            .hasSize(1)
            .hasRecord(0, v -> v
                .hasKey("test".getBytes())
                .hasValue("test-value")
            )
        );
    }

    @Test
    public void testProcessRecordsAndConsumer() {
        var handler = compile("""
            public class KafkaListener {
                @KafkaIncoming("test.config.path")
                public void process(Consumer<?, ?> consumer, ConsumerRecords<String, String> event) {
                }
            }
            """)
            .recordsHandler(String.class, String.class);

        handler.handle(record("test", "test-value"), i -> i
            .assertConsumer(0)
            .assertRecords(1)
            .hasSize(1)
            .hasRecord(0, v -> v
                .hasKey("test")
                .hasValue("test-value")
            )
        );
    }

    @Test
    public void testProcessRecordsAndConsumerAndTelemetry() {
        var handler = compile("""
            public class KafkaListener {
                @KafkaIncoming("test.config.path")
                public void process(Consumer<?, ?> consumer, ConsumerRecords<byte[], String> event, KafkaConsumerTelemetry.KafkaConsumerRecordsTelemetryContext<?, ?> telemetry) {
                }
            }
            """)
            .recordsHandler(byte[].class, String.class);

        handler.handle(record("test".getBytes(), "test-value"), i -> i
            .assertConsumer(0)
            .assertTelemetry(2)
            .assertRecords(1)
            .hasSize(1)
            .hasRecord(0, v -> v
                .hasKey("test".getBytes())
                .hasValue("test-value")
            )
        );
    }

    @Test
    public void testProcessRecordsAndTelemetry() {
        var handler = compile("""
            public class KafkaListener {
                @KafkaIncoming("test.config.path")
                public void process(KafkaConsumerTelemetry.KafkaConsumerRecordsTelemetryContext<?, ?> telemetry, ConsumerRecords<byte[], String> event) {
                }
            }
            """)
            .recordsHandler(byte[].class, String.class);

        handler.handle(record("test".getBytes(), "test-value"), i -> i
            .assertTelemetry(0)
            .assertRecords(1)
            .hasSize(1)
            .hasRecord(0, v -> v
                .hasKey("test".getBytes())
                .hasValue("test-value")
            )
        );
    }
}
