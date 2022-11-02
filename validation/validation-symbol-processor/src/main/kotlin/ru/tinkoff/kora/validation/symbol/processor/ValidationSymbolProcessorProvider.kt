package ru.tinkoff.kora.validation.symbol.processor

import com.google.devtools.ksp.KspExperimental
import com.google.devtools.ksp.processing.SymbolProcessor
import com.google.devtools.ksp.processing.SymbolProcessorEnvironment
import com.google.devtools.ksp.processing.SymbolProcessorProvider
import com.squareup.kotlinpoet.ksp.KotlinPoetKspPreview

@KspExperimental
@KotlinPoetKspPreview
class ValidationSymbolProcessorProvider : SymbolProcessorProvider {

    override fun create( environment: SymbolProcessorEnvironment ): SymbolProcessor {
        return ValidationSymbolProcessor(environment)
    }
}
