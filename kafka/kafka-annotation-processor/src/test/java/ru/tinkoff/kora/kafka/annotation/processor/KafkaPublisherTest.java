package ru.tinkoff.kora.kafka.annotation.processor;

import org.junit.jupiter.api.Test;
import ru.tinkoff.kora.annotation.processor.common.AbstractAnnotationProcessorTest;
import ru.tinkoff.kora.kafka.annotation.processor.producer.KafkaPublisherAnnotationProcessor;

import java.util.List;

public class KafkaPublisherTest extends AbstractAnnotationProcessorTest {
    @Test
    public void testProducer() {
        this.compile(List.of(new KafkaPublisherAnnotationProcessor()), """
            @ru.tinkoff.kora.kafka.common.annotation.KafkaPublisher("test")
            public interface TestProducer extends org.apache.kafka.clients.producer.Producer<byte[] ,byte[]> {}
            """);
        this.compileResult.assertSuccess();
    }

    @Test
    public void testProducerWithKeyTag() {
        this.compile(List.of(new KafkaPublisherAnnotationProcessor()), """
            @ru.tinkoff.kora.kafka.common.annotation.KafkaPublisher("test")
            public interface TestProducer extends org.apache.kafka.clients.producer.Producer<@Tag(String.class) String ,byte[]> {}
            """);
        this.compileResult.assertSuccess();
    }

    @Test
    public void testTransactionalProducer() {
        this.compile(List.of(new KafkaPublisherAnnotationProcessor()), """
            @ru.tinkoff.kora.kafka.common.annotation.KafkaPublisher("test")
            public interface TestProducer extends ru.tinkoff.kora.kafka.common.producer.TransactionalProducer<byte[] ,byte[]> {}
            """);
        this.compileResult.assertSuccess();
    }
}
