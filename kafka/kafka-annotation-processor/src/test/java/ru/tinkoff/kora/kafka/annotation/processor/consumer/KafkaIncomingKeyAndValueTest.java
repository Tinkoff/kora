package ru.tinkoff.kora.kafka.annotation.processor.consumer;

import org.junit.jupiter.api.Test;
import ru.tinkoff.kora.kafka.common.exceptions.RecordKeyDeserializationException;
import ru.tinkoff.kora.kafka.common.exceptions.RecordValueDeserializationException;

public class KafkaIncomingKeyAndValueTest extends AbstractKafkaIncomingAnnotationProcessorTest {
    @Test
    public void testProcessValue() {
        var handler = compile("""
            public class KafkaListener {
                @KafkaIncoming("test.config.path")
                public void process(String value) {
                }
            }
            """)
            .handler(byte[].class, String.class);

        handler.handle(record("test".getBytes(), "test-value"), i -> i.assertValue(0, "test-value"));

        handler.handle(errorKey("test-value"), i -> i.assertValue(0, "test-value"));

        handler.handle(errorValue(), RecordValueDeserializationException.class);
    }

    @Test
    public void testProcessKeyAndValue() {
        var handler = compile("""
            public class KafkaListener {
                @KafkaIncoming("test.config.path")
                public void process(String key, String value) {
                }
            }
            """)
            .handler(String.class, String.class);

        handler.handle(record("test", "test-value"), i -> {
            i.assertKey(0, "test");
            i.assertValue(1, "test-value");
        });

        handler.handle(errorKey(""), RecordKeyDeserializationException.class);
        handler.handle(errorValue(), RecordValueDeserializationException.class);
    }

    @Test
    public void testProcessValueAndValueException() {
        var handler = compile("""
            public class KafkaListener {
                @KafkaIncoming("test.config.path")
                public void process(String value, RecordValueDeserializationException exception) {
                }
            }
            """)
            .handler(byte[].class, String.class);

        handler.handle(record("test".getBytes(), "test-value"), i -> {
            i.assertValue(0, "test-value");
            i.assertNoException(1);
        });

        handler.handle(errorKey("test-value"), i -> {
            i.assertValue(0, "test-value");
            i.assertNoException(1);
        });

        handler.handle(errorValue(), i -> {
            i.assertNoValue(0);
            i.assertValueException(1);
        });
    }

    @Test
    public void testProcessValueAndException() {
        var handler = compile("""
            public class KafkaListener {
                @KafkaIncoming("test.config.path")
                public void process(String value, Exception exception) {
                }
            }
            """)
            .handler(byte[].class, String.class);

        handler.handle(record("test".getBytes(), "test-value"), i -> {
            i.assertValue(0, "test-value");
            i.assertNoException(1);
        });

        handler.handle(errorKey("test-value"), i -> {
            i.assertValue(0, "test-value");
            i.assertNoException(1);
        });

        handler.handle(errorValue(), i -> {
            i.assertNoValue(0);
            i.assertValueException(1);
        });
    }

    @Test
    public void testProcessKeyValueAndException() {
        var handler = compile("""
            public class KafkaListener {
                @KafkaIncoming("test.config.path")
                public void process(String key, String value, Exception exception) {
                }
            }
            """)
            .handler(String.class, String.class);

        handler.handle(record("test", "test-value"), i -> {
            i.assertKey(0, "test");
            i.assertValue(1, "test-value");
            i.assertNoException(2);
        });

        handler.handle(errorKey("test-value"), i -> {
            i.assertNoKey(0);
            i.assertNoValue(1);
            i.assertKeyException(2);
        });

        handler.handle(errorValue(), i -> {
            i.assertNoKey(0);
            i.assertNoValue(1);
            i.assertValueException(2);
        });
    }

    @Test
    public void testProcessValueAndConsumer() {
        var handler = compile("""
            public class KafkaListener {
                @KafkaIncoming("test.config.path")
                public void process(Consumer<?, ?> consumer, String value, Exception exception) {
                }
            }
            """)
            .handler(byte[].class, String.class);

        handler.handle(record("test".getBytes(), "test-value"), i -> {
            i.assertConsumer(0);
            i.assertValue(1, "test-value");
            i.assertNoException(2);
        });

        handler.handle(errorKey("test-value"), i -> {
            i.assertConsumer(0);
            i.assertValue(1, "test-value");
            i.assertNoException(2);
        });

        handler.handle(errorValue(), i -> {
            i.assertConsumer(0);
            i.assertNoValue(1);
            i.assertValueException(2);
        });
    }
}
