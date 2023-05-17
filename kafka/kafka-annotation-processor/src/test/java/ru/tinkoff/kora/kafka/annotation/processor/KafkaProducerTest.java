package ru.tinkoff.kora.kafka.annotation.processor;

import org.junit.jupiter.api.Test;
import ru.tinkoff.kora.annotation.processor.common.AbstractAnnotationProcessorTest;
import ru.tinkoff.kora.kafka.annotation.processor.producer.KafkaProducerAnnotationProcessor;

import java.util.List;

public class KafkaProducerTest extends AbstractAnnotationProcessorTest {
    @Test
    public void testProducer() {
        this.compile(List.of(new KafkaProducerAnnotationProcessor()), """
            @ru.tinkoff.kora.kafka.common.annotation.KafkaProducer("test")
            public interface TestProducer extends org.apache.kafka.clients.producer.Producer<byte[] ,byte[]> {}
            """);
        this.compileResult.assertSuccess();
    }

    @Test
    public void testProducerWithKeyTag() {
        this.compile(List.of(new KafkaProducerAnnotationProcessor()), """
            @ru.tinkoff.kora.kafka.common.annotation.KafkaProducer("test")
            public interface TestProducer extends org.apache.kafka.clients.producer.Producer<@Tag(String.class) String ,byte[]> {}
            """);
        this.compileResult.assertSuccess();
    }

    @Test
    public void testTransactionalProducer() {
        this.compile(List.of(new KafkaProducerAnnotationProcessor()), """
            @ru.tinkoff.kora.kafka.common.annotation.KafkaProducer("test")
            public interface TestProducer extends ru.tinkoff.kora.kafka.common.producer.TransactionalProducer<byte[] ,byte[]> {}
            """);
        this.compileResult.assertSuccess();
    }
}
