package ru.tinkoff.kora.kafka.annotation.processor.consumer;

import ru.tinkoff.kora.annotation.processor.common.AbstractKoraProcessor;
import ru.tinkoff.kora.annotation.processor.common.ProcessingErrorException;
import ru.tinkoff.kora.kafka.annotation.processor.KafkaClassNames;

import javax.annotation.processing.RoundEnvironment;
import javax.lang.model.element.Element;
import javax.lang.model.element.TypeElement;
import java.io.IOException;
import java.util.Set;
import java.util.stream.Collectors;

public class KafkaIncomingAnnotationProcessor extends AbstractKoraProcessor {

    @Override
    public Set<String> getSupportedAnnotationTypes() {
        return Set.of(KafkaClassNames.kafkaIncoming.canonicalName());
    }

    @Override
    public boolean process(Set<? extends TypeElement> annotations, RoundEnvironment roundEnv) {
        var kafkaIncoming = this.elements.getTypeElement(KafkaClassNames.kafkaIncoming.canonicalName());
        var typeElements = roundEnv.getElementsAnnotatedWith(kafkaIncoming)
            .stream()
            .map(Element::getEnclosingElement)
            .map(TypeElement.class::cast)
            .collect(Collectors.toSet());
        for (var element : typeElements) {
            try {
                processController(element);
            } catch (ProcessingErrorException e) {
                e.printError(this.processingEnv);
            } catch (IOException e) {
                throw new RuntimeException(e);
            }
        }

        return false;
    }

    private void processController(TypeElement controller) throws IOException {
        var methodGenerator = new KafkaConsumerHandlerGenerator();
        var kafkaConfigGenerator = new KafkaConsumerConfigGenerator();
        var kafkaConsumerContainerGenerator = new KafkaConsumerContainerGenerator();
        var generator = new KafkaConsumerModuleGenerator(processingEnv, methodGenerator, kafkaConfigGenerator, kafkaConsumerContainerGenerator);
        var file = generator.generateModule(controller);
        file.writeTo(this.processingEnv.getFiler());
    }
}
