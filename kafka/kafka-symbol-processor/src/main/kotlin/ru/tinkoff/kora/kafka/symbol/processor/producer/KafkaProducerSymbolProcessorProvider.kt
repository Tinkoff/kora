package ru.tinkoff.kora.kafka.symbol.processor.producer

import com.google.devtools.ksp.processing.SymbolProcessorEnvironment
import com.google.devtools.ksp.processing.SymbolProcessorProvider

class KafkaProducerSymbolProcessorProvider : SymbolProcessorProvider {
    override fun create(environment: SymbolProcessorEnvironment) = KafkaProducerSymbolProcessor(environment)
}
