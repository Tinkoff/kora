package ru.tinkoff.kora.kafka.annotation.processor.consumer;

import org.junit.jupiter.api.Test;

public class KafkaIncomingRecordTest extends AbstractKafkaIncomingAnnotationProcessorTest {
    @Test
    public void testProcessRecord() {
        var handler = compile("""
            public class KafkaListener {
                @KafkaIncoming("test.config.path")
                public void process(ConsumerRecord<String, String> event) {
                }
            }
            """)
            .handler(String.class, String.class);

        handler.handle(record("test", "test-value"), i -> i
            .assertRecord(0)
            .hasKey("test")
            .hasValue("test-value"));
    }

    @Test
    public void testProcessRecordAnyKeyType() {
        var handler = compile("""
            public class KafkaListener {
                @KafkaIncoming("test.config.path")
                public void process(ConsumerRecord<?, String> event) {
                }
            }
            """)
            .handler(byte[].class, String.class);

        handler.handle(record("test".getBytes(), "test-value"), i -> i
            .assertRecord(0)
            .hasKey("test".getBytes())
            .hasValue("test-value")
        );
        handler.handle(errorKey("test-value"), i -> i
            .assertRecord(0)
            .hasKeyError()
            .hasValue("test-value")
        );
        handler.handle(errorValue("test".getBytes()), i -> i
            .assertRecord(0)
            .hasKey("test".getBytes())
            .hasValueError());
    }

    @Test
    public void testProcessRecordAndConsumer() {
        var handler = compile("""
            public class KafkaListener {
                @KafkaIncoming("test.config.path")
                public void process(Consumer<String, String> consumer, ConsumerRecord<String, String> event) {
                }
            }
            """)
            .handler(String.class, String.class);

        handler.handle(record("test", "test-value"), i -> i
            .assertConsumer(0)
            .assertRecord(1)
            .hasKey("test")
            .hasValue("test-value")
        );
        handler.handle(errorKey("test-value"), i -> i
            .assertConsumer(0)
            .assertRecord(1)
            .hasKeyError()
            .hasValue("test-value")
        );
        handler.handle(errorValue("test"), i -> i
            .assertConsumer(0)
            .assertRecord(1)
            .hasKey("test")
            .hasValueError());
    }

    @Test
    public void testProcessRecordAndKeyParseException() {
        var handler = compile("""
            public class KafkaListener {
                @KafkaIncoming("test.config.path")
                public void process(ConsumerRecord<String, String> event, RecordKeyDeserializationException exception) {
                }
            }
            """)
            .handler(String.class, String.class);

        handler.handle(record("test", "test-value"), i -> i
            .assertNoException(1)
            .assertRecord(0)
            .hasKey("test")
            .hasValue("test-value")
        );
        handler.handle(errorKey("test-value"), i -> i
            .assertKeyException(1)
            .assertRecord(0)
            .hasKeyError()
            .hasValue("test-value")
        );
        handler.handle(errorValue("test"), i -> i
            .assertNoException(1)
            .assertRecord(0)
            .hasKey("test")
            .hasValueError());
    }

    @Test
    public void testProcessRecordAndValueParseException() {
        var handler = compile("""
            public class KafkaListener {
                @KafkaIncoming("test.config.path")
                public void process(ConsumerRecord<String, String> event, RecordValueDeserializationException exception) {
                }
            }
            """)
            .handler(String.class, String.class);

        handler.handle(record("test", "test-value"), i -> i
            .assertNoException(1)
            .assertRecord(0)
            .hasKey("test")
            .hasValue("test-value")
        );
        handler.handle(errorKey("test-value"), i -> i
            .assertNoException(1)
            .assertRecord(0)
            .hasKeyError()
            .hasValue("test-value")
        );
        handler.handle(errorValue("test"), i -> i
            .assertValueException(1)
            .assertRecord(0)
            .hasKey("test")
            .hasValueError());
    }

    @Test
    public void testProcessRecordAndParseException() {
        var handler = compile("""
            public class KafkaListener {
                @KafkaIncoming("test.config.path")
                public void process(ConsumerRecord<String, String> event, Exception exception) {
                }
            }
            """)
            .handler(String.class, String.class);

        handler.handle(record("test", "test-value"), i -> i
            .assertNoException(1)
            .assertRecord(0)
            .hasKey("test")
            .hasValue("test-value")
        );
        handler.handle(errorKey("test-value"), i -> i
            .assertKeyException(1)
            .assertRecord(0)
            .hasKeyError()
            .hasValue("test-value")
        );
        handler.handle(errorValue("test"), i -> i
            .assertValueException(1)
            .assertRecord(0)
            .hasKey("test")
            .hasValueError()
        );
    }

    @Test
    public void testProcessRecordAndParseThrowable() {
        var handler = compile("""
            public class KafkaListener {
                @KafkaIncoming("test.config.path")
                public void process(ConsumerRecord<String, String> event, Throwable exception) {
                }
            }
            """)
            .handler(String.class, String.class);

        handler.handle(record("test", "test-value"), i -> i
            .assertNoException(1)
            .assertRecord(0)
            .hasKey("test")
            .hasValue("test-value")
        );
        handler.handle(errorKey("test-value"), i -> i
            .assertKeyException(1)
            .assertRecord(0)
            .hasKeyError()
            .hasValue("test-value")
        );
        handler.handle(errorValue("test"), i -> i
            .assertValueException(1)
            .assertRecord(0)
            .hasKey("test")
            .hasValueError()
        );
    }

}
