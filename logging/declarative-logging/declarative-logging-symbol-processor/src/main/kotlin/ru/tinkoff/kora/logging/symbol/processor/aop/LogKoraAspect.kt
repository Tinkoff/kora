package ru.tinkoff.kora.logging.symbol.processor.aop

import com.google.devtools.ksp.KspExperimental
import com.google.devtools.ksp.getAnnotationsByType
import com.google.devtools.ksp.getClassDeclarationByName
import com.google.devtools.ksp.isAnnotationPresent
import com.google.devtools.ksp.processing.Resolver
import com.google.devtools.ksp.symbol.KSFunctionDeclaration
import com.google.devtools.ksp.symbol.KSType
import com.squareup.kotlinpoet.CodeBlock
import org.slf4j.ILoggerFactory
import org.slf4j.Logger
import org.slf4j.event.Level
import ru.tinkoff.kora.aop.symbol.processor.KoraAspect
import ru.tinkoff.kora.logging.annotation.Log
import ru.tinkoff.kora.logging.common.arg.StructuredArgument
import ru.tinkoff.kora.logging.symbol.processor.aop.data.MethodData
import ru.tinkoff.kora.logging.symbol.processor.aop.data.MethodParameterData
import kotlin.reflect.KClass
import ru.tinkoff.kora.logging.annotation.Log.`in` as LogIn
import ru.tinkoff.kora.logging.annotation.Log.out as LogOut

@KspExperimental
class LogKoraAspect(resolver: Resolver) : KoraAspect {

    private companion object {
        private const val RESULT_FIELD_NAME = "__result"
        private const val DATA_IN_FIELD_NAME = "__dataIn"
        private const val DATA_OUT_FIELD_NAME = "__dataOut"
        private const val DATA_PARAMETER_NAME = "data"
        private const val OUT_PARAMETER_NAME = "out"
        private const val MARKER_GENERATOR_PARAMETER_NAME = "gen"

        private const val MESSAGE_IN = ">"
        private const val MESSAGE_OUT = "<"
    }

    private val iLoggerFactoryType = resolver.getClassDeclarationByClass(ILoggerFactory::class).asType(emptyList())
    private val loggerType = resolver.getClassDeclarationByClass(Logger::class).asType(emptyList())
    private val unitType = resolver.getClassDeclarationByClass(Unit::class).asStarProjectedType()
    private val voidType = resolver.getClassDeclarationByClass(Void::class).asStarProjectedType()

    override fun getSupportedAnnotationTypes(): Set<String> {
        return setOf(Log::class.java.canonicalName, LogIn::class.java.canonicalName, LogOut::class.java.canonicalName)
    }

    override fun apply(
        function: KSFunctionDeclaration,
        superCall: String,
        aspectContext: KoraAspect.AspectContext
    ): KoraAspect.ApplyResult {
        val loggerFactoryFieldName = aspectContext.fieldFactory.constructorParam(iLoggerFactoryType, emptyList())
        val declarationName = function.parentDeclaration?.qualifiedName?.asString()
        val loggerName = "${declarationName}.${function.simpleName.getShortName()}"
        val loggerFieldName = aspectContext.fieldFactory.constructorInitialized(
            loggerType,
            CodeBlock.of("%N.getLogger(%S)", loggerFactoryFieldName, loggerName)
        )

        val methodData = resolveMethodData(function, loggerFieldName, superCall)
        val result = generator(methodData) {
            generateInputLog()
            generateOutputLog()
        }

        return KoraAspect.ApplyResult.MethodBody(result)
    }

    private fun resolveMethodData(
        function: KSFunctionDeclaration,
        loggerName: String,
        superCall: String
    ): MethodData {
        val (inputLogLevel, outputLogLevel) = resolveLoggerLevels(function)
        val resultLogLevel = resolveResultLogLevel(outputLogLevel, function)
        val returnType = function.returnType?.resolve()!!

        return MethodData(
            superCall = superCall,
            loggerName = loggerName,
            inputLogLevel = inputLogLevel,
            outputLogLevel = outputLogLevel,
            resultLogLevel = resultLogLevel,
            parameters = resolveMethodParametersData(inputLogLevel, function),
            isVoid = returnType.isVoid()
        )
    }

    private fun GeneratorSpec.generateInputLog() {
        if (methodData.parameters.isEmpty()) {
            logInput()
        } else {
            val parametersByLevel = methodData.parameters
                .filter { it.logLevel != null }
                .groupBy { it.logLevel!! }
                .toList()
                .sortedBy { (level, _) -> level }
                .takeIf { it.isNotEmpty() }
                ?: return

            val minimalLogLevel = parametersByLevel.minOf { (level, _) -> level }

            checkLogLevel(minimalLogLevel, false) {
                codeBlock.beginControlFlow(
                    "val %L = %T.marker(%S) { gen ->",
                    DATA_IN_FIELD_NAME,
                    StructuredArgument::class,
                    DATA_PARAMETER_NAME
                )
            }

            parametersByLevel.forEach { (level, parameters) ->
                if (level <= methodData.inputLogLevel) {
                    parameters.forEach { parameter ->
                        appendFieldToMarkerGenerator(parameter.name, parameter.name)
                    }
                } else {
                    checkLogLevel(level) {
                        parameters.forEach { parameter ->
                            appendFieldToMarkerGenerator(parameter.name, parameter.name)
                        }
                    }
                }
            }

            codeBlock.endControlFlow()

            logWithMarker(methodData.inputLogLevel, DATA_IN_FIELD_NAME, MESSAGE_IN)

            if (minimalLogLevel > methodData.inputLogLevel) {
                codeBlock.nextControlFlow("else")
                logInput()
            }

            codeBlock.endControlFlow()
        }
    }

    private fun GeneratorSpec.logInput() {
        log(methodData.inputLogLevel, MESSAGE_IN)
    }

    private fun GeneratorSpec.logOutput() {
        log(methodData.outputLogLevel, MESSAGE_OUT)
    }

    private fun GeneratorSpec.logOutput(outputField: String) {
        if (methodData.resultLogLevel == null) {
            logOutput()
        } else {
            checkLogLevel(methodData.resultLogLevel, false) {
                codeBlock.beginControlFlow(
                    "val %L = %T.marker(%S) { gen -> ",
                    DATA_OUT_FIELD_NAME,
                    StructuredArgument::class,
                    DATA_PARAMETER_NAME
                )
                appendFieldToMarkerGenerator(OUT_PARAMETER_NAME, outputField)
                codeBlock.endControlFlow()
                logWithMarker(methodData.outputLogLevel, DATA_OUT_FIELD_NAME, MESSAGE_OUT)
            }

            if (methodData.resultLogLevel >= methodData.outputLogLevel) {
                codeBlock.nextControlFlow("else")
                logOutput()
            }

            codeBlock.endControlFlow()
        }
    }

    private fun GeneratorSpec.appendFieldToMarkerGenerator(fieldName: String, parameterName: String) {
        codeBlock.addStatement(
            "%L.writeStringField(%S, %L.toString())",
            MARKER_GENERATOR_PARAMETER_NAME,
            fieldName,
            parameterName
        )
    }

    private fun GeneratorSpec.generateOutputLog() {
        if (methodData.outputLogLevel == null) {
            superCall()
        } else {
            if (methodData.isVoid) {
                superCall()
                logOutput()
            } else {
                storedSuperCall().let { output ->
                    logOutput(output)
                    returning(output)
                }
            }
        }
    }

    private fun resolveMethodParametersData(
        methodOutLogLevel: Level?,
        function: KSFunctionDeclaration,
    ): List<MethodParameterData> {
        return if (methodOutLogLevel == null) {
            emptyList()
        } else {
            function.parameters.map {
                MethodParameterData(
                    name = it.name!!.asString(),
                    logLevel = if (it.isAnnotationPresent(Log.off::class)) {
                        null
                    } else {
                        val parameterLogLevel = it.getAnnotationsByType(Log::class).firstOrNull()?.value ?: Level.DEBUG
                        maxOf(parameterLogLevel, methodOutLogLevel)
                    }
                )
            }
        }
    }

    private fun resolveLoggerLevels(function: KSFunctionDeclaration): Pair<Level?, Level?> {
        val baseLogAnnotation = function.getAnnotationsByType(Log::class).firstOrNull()

        return if (baseLogAnnotation == null) {
            val inputLogLevel = function.getAnnotationsByType(Log.`in`::class).firstOrNull()?.value
            val outputLogLevel = function.getAnnotationsByType(Log.out::class).firstOrNull()?.value

            inputLogLevel to outputLogLevel
        } else {
            baseLogAnnotation.value to baseLogAnnotation.value
        }
    }

    private fun resolveResultLogLevel(outLogLevel: Level?, function: KSFunctionDeclaration): Level? {
        val logOffAnnotation = function.getAnnotationsByType(Log.off::class).firstOrNull()

        if (logOffAnnotation != null) {
            return null
        }

        val logResultValue = function.getAnnotationsByType(Log.result::class).firstOrNull()?.value

        return if (outLogLevel == null && logResultValue == null) {
            null
        } else {
            logResultValue ?: Level.DEBUG
        }
    }

    private fun GeneratorSpec.superCall() {
        codeBlock.addStatement("%L", prepareSuperCall())
    }

    private fun GeneratorSpec.storedSuperCall(): String {
        codeBlock.addStatement("val %L = %L", RESULT_FIELD_NAME, prepareSuperCall())
        return RESULT_FIELD_NAME
    }

    private fun GeneratorSpec.returning(propertyName: String) {
        codeBlock.addStatement("return·%L", propertyName)
    }

    private fun GeneratorSpec.prepareSuperCall(): String {
        return "${methodData.superCall}(${methodData.parameters.joinToString(", ") { it.name }})"
    }

    private fun GeneratorSpec.log(logLevel: Level?, message: String) {
        log(logLevel) { logMethod ->
            codeBlock.addStatement("%L.%L(%S)", methodData.loggerName, logMethod, message)
        }
    }

    private fun GeneratorSpec.logWithMarker(logLevel: Level?, marker: String, message: String) {
        log(logLevel) { logMethod ->
            codeBlock.addStatement("%L.%L(%L, %S)", methodData.loggerName, logMethod, marker, message)
        }
    }

    private fun log(logLevel: Level?, action: (String) -> Unit) {
        logLevel?.let { action(it.name.lowercase()) }
    }

    private fun GeneratorSpec.checkLogLevel(logLevel: Level?, forceEndFlow: Boolean = true, action: () -> Unit) {
        val checkLogLevelMethod = logLevel?.let {
            "is${it.name.lowercase().replaceFirstChar { c -> c.uppercase() }}Enabled"
        }

        if (checkLogLevelMethod != null) {
            codeBlock.beginControlFlow("if (%L.%L)", methodData.loggerName, checkLogLevelMethod)
            action()
            if (forceEndFlow) {
                codeBlock.endControlFlow()
            }
        } else {
            action()
        }
    }

    private fun KSType.isVoid(): Boolean {
        return unitType == this || voidType == this
    }

    private fun generator(methodData: MethodData, builderAction: GeneratorSpec.() -> Unit): CodeBlock {
        return GeneratorSpec(methodData).also(builderAction).build()
    }

    private fun Resolver.getClassDeclarationByClass(type: KClass<*>) =
        getClassDeclarationByName(type.java.canonicalName)
        ?: error("Could not found class ${type.java.canonicalName} in classpath")
}

class GeneratorSpec(val methodData: MethodData) {
    val codeBlock = CodeBlock.builder()
    fun build() = codeBlock.build()
}
