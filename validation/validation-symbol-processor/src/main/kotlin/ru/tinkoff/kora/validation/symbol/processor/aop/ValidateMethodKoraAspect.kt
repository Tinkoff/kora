package ru.tinkoff.kora.validation.symbol.processor.aop

import com.google.devtools.ksp.processing.Resolver
import com.google.devtools.ksp.symbol.KSFunctionDeclaration
import com.google.devtools.ksp.symbol.KSValueParameter
import com.squareup.kotlinpoet.ClassName
import com.squareup.kotlinpoet.CodeBlock
import reactor.core.publisher.Flux
import reactor.core.publisher.Mono
import ru.tinkoff.kora.aop.symbol.processor.KoraAspect
import ru.tinkoff.kora.ksp.common.FunctionUtils.isFlow
import ru.tinkoff.kora.ksp.common.FunctionUtils.isFlux
import ru.tinkoff.kora.ksp.common.FunctionUtils.isFuture
import ru.tinkoff.kora.ksp.common.FunctionUtils.isMono
import ru.tinkoff.kora.ksp.common.FunctionUtils.isSuspend
import ru.tinkoff.kora.ksp.common.FunctionUtils.isVoid
import ru.tinkoff.kora.ksp.common.exception.ProcessingErrorException
import ru.tinkoff.kora.validation.symbol.processor.*
import java.util.*
import java.util.concurrent.Future

class ValidateMethodKoraAspect(private val resolver: Resolver) : KoraAspect {

    private val validateType = ClassName.bestGuess("ru.tinkoff.kora.validation.common.annotation.Validate")

    override fun getSupportedAnnotationTypes(): Set<String> {
        return setOf(validateType.canonicalName)
    }

    override fun apply(method: KSFunctionDeclaration, superCall: String, aspectContext: KoraAspect.AspectContext): KoraAspect.ApplyResult {
        val validationOutputCode = buildValidationOutputCode(method, aspectContext) ?: return KoraAspect.ApplyResult.Noop.INSTANCE

        if (method.isFuture()) {
            throw ProcessingErrorException("@Validate Return Value can't be applied for types assignable from ${Future::class.java}", method)
        } else if (method.isMono()) {
            throw ProcessingErrorException("@Validate Return Value can't be applied for types assignable from ${Mono::class.java}", method)
        } else if (method.isFlux()) {
            throw ProcessingErrorException("@Validate Return Value can't be applied for types assignable from ${Flux::class.java}", method)
        } else if (method.isVoid()) {
            throw ProcessingErrorException("@Validate Return Value can't be applied for types assignable from ${Void::class.java}", method)
        }

        val validationInputCode = buildValidationInputCode(method, aspectContext) ?: return KoraAspect.ApplyResult.Noop.INSTANCE

        val body = if (method.isFlow()) {
            buildBodyFlow(method, superCall, validationOutputCode, validationInputCode)
        } else if (method.isSuspend()) {
            buildBodySuspend(method, superCall, validationOutputCode, validationInputCode)
        } else {
            buildBodySync(method, superCall, validationOutputCode, validationInputCode)
        }

        return KoraAspect.ApplyResult.MethodBody(body)
    }

    private fun buildValidationOutputCode(
        method: KSFunctionDeclaration,
        aspectContext: KoraAspect.AspectContext
    ): CodeBlock? {
        val returnType = if (method.isFlow())
            method.returnType!!.asType().generic.first()
        else
            method.returnType!!.asType()

        val constraints = ValidUtils.getConstraints(method.returnType!!, method.annotations)
        val validates = if (method.annotations.any { a -> a.annotationType.toString() == VALID_TYPE.canonicalName })
            listOf(Validated(returnType))
        else
            emptyList()

        if (constraints.isEmpty() && validates.isEmpty()) {
            return null
        }

        val resolvedReturnType = method.returnType!!.resolve()
        val isNullable = resolvedReturnType.isMarkedNullable

        val builder = CodeBlock.builder()
        if (isNullable) {
            builder.beginControlFlow("if(_result != null)")
        }

        builder.addStatement("val _returnValueViolations = %T<%T>(%L)", ArrayList::class.java, VIOLATION_TYPE, method.parameters.size * 2)

        for (validated in validates) {
            val validatorType = validated.validator().asKSType(resolver)
            val validatorField = aspectContext.fieldFactory.constructorParam(validatorType, listOf())
            builder.addStatement("_returnValueViolations.addAll(%N.validate(_result))", validatorField)
        }

        for (constraint in constraints) {
            val factoryType = constraint.factory.type.asKSType(resolver)
            val constraintFactory = aspectContext.fieldFactory.constructorParam(factoryType, listOf())
            val constraintType = constraint.factory.validator().asKSType(resolver)

            val parameters = CodeBlock.of(constraint.factory.parameters.values.asSequence()
                .map { fp -> CodeBlock.of("%L", fp) }
                .joinToString(", ", "(", ")"))

            val createCodeBlock = CodeBlock.builder()
                .add("%N.create", constraintFactory)
                .add(parameters)
                .build()

            val constraintField = aspectContext.fieldFactory.constructorInitialized(constraintType, createCodeBlock)
            builder.addStatement("_returnValueViolations.addAll(%N.validate(_result))", constraintField)
        }

        builder.beginControlFlow("if (!_returnValueViolations.isEmpty())")
            .addStatement("throw %T(_returnValueViolations)", EXCEPTION_TYPE)
            .endControlFlow()

        if (isNullable) {
            builder.endControlFlow()
        }

        return builder.build()
    }

    private fun buildValidationInputCode(
        method: KSFunctionDeclaration,
        aspectContext: KoraAspect.AspectContext
    ): CodeBlock? {
        if (method.parameters.none { it.isValidatable() }) {
            return null
        }

        val builder = CodeBlock.builder()
            .addStatement("val _argumentsViolations = %T<%T>(%L)", ArrayList::class.java, VIOLATION_TYPE, method.parameters.size * 2)

        for (parameter in method.parameters.filter { it.isValidatable() }) {
            val constraints = ValidUtils.getConstraints(parameter.type, parameter.annotations)
            for (constraint in constraints) {
                val factoryType = constraint.factory.type.asKSType(resolver)
                val constraintFactory = aspectContext.fieldFactory.constructorParam(factoryType, listOf())
                val constraintType = constraint.factory.validator().asKSType(resolver)

                val parameters = CodeBlock.of(constraint.factory.parameters.values.asSequence()
                    .map { fp -> CodeBlock.of("%L", fp) }
                    .joinToString(", ", "(", ")"))

                val createCodeBlock = CodeBlock.builder()
                    .add("%N.create", constraintFactory)
                    .add(parameters)
                    .build()

                val constraintField = aspectContext.fieldFactory.constructorInitialized(constraintType, createCodeBlock)
                if (parameter.type.resolve().isMarkedNullable) {
                    builder.beginControlFlow("if(_result != null)")
                    builder.addStatement("_argumentsViolations.addAll(%N.validate(_result))", constraintField)
                    builder.endControlFlow()
                } else {
                    builder.addStatement("_argumentsViolations.addAll(%N.validate(_result))", constraintField)
                }
            }

            val validates = getValidForArguments(parameter)
            for (validated in validates) {
                val validatorType = validated.validator().asKSType(resolver)
                val validatorField = aspectContext.fieldFactory.constructorParam(validatorType, listOf())
                if (parameter.type.resolve().isMarkedNullable) {
                    builder.beginControlFlow("if(_result != null)")
                    builder.addStatement("_argumentsViolations.addAll(%N.validate(_result))", validatorField)
                    builder.endControlFlow()
                } else {
                    builder.addStatement("_argumentsViolations.addAll(%N.validate(_result))", validatorField)
                }
            }

            builder.beginControlFlow("if (!_argumentsViolations.isEmpty())")
                .addStatement("throw %T(_argumentsViolations)", EXCEPTION_TYPE)
                .endControlFlow()
        }

        return builder.build()
    }

    private fun KSValueParameter.isValidatable(): Boolean {
        for (annotation in this.annotations) {
            val annotationType = annotation.annotationType
            if (annotationType.toString() == VALID_TYPE.canonicalName) {
                return true
            }

            for (innerAnnotation in annotationType.resolve().annotations) {
                if (innerAnnotation.annotationType.toString() == VALIDATED_BY_TYPE.canonicalName) {
                    return true
                }
            }
        }
        return false
    }

    private fun getValidForArguments(parameter: KSValueParameter): List<Validated> {
        return if (parameter.annotations.any { it.annotationType.toString() == VALID_TYPE.canonicalName }) {
            listOf(Validated(parameter.type.asType()))
        } else
            emptyList()
    }

    private fun buildBodySync(
        method: KSFunctionDeclaration,
        superCall: String,
        validationOutput: CodeBlock?,
        validationInput: CodeBlock?
    ): CodeBlock {
        val superMethod = buildMethodCall(method, superCall)
        val builder = CodeBlock.builder()
        if (method.isVoid()) {
            if(validationInput != null) {
                builder.add(validationInput).add("\n")
            }

            builder.add("%L\n".trimIndent(), superMethod.toString())

            if (validationOutput != null) {
                builder.add(validationOutput)
            }
        } else {
            if(validationInput != null) {
                builder.add(validationInput).add("\n")
            }

            builder.add("val _result = %L\n".trimIndent(), superMethod.toString())

            if (validationOutput != null) {
                builder.add(validationOutput)
            }

            builder.add("return _result")
        }

        return builder.build()
    }

    private fun buildBodySuspend(
        method: KSFunctionDeclaration,
        superCall: String,
        validationOutput: CodeBlock?,
        validationInput: CodeBlock?
    ): CodeBlock {
        return buildBodySync(method, superCall, validationOutput, validationInput)
    }

    private fun buildBodyFlow(
        method: KSFunctionDeclaration,
        superCall: String,
        validationOutput: CodeBlock?,
        validationInput: CodeBlock?
    ): CodeBlock {
        val superMethod = buildMethodCall(method, superCall)
        return CodeBlock.builder()
            .add(
                """
                return %L
                    .map(_result -> {
                    
                    """.trimIndent(), superMethod.toString()
            )
            .indent().indent().indent().indent()
            .add(validationOutput)
            .add("return _result")
            .unindent().unindent().unindent().unindent()
            .add(
                """
                                
                });
                                
                """.trimIndent()
            )
            .build()
    }

    private fun buildMethodCall(method: KSFunctionDeclaration, call: String): CodeBlock {
        return CodeBlock.of(method.parameters.asSequence().map { p -> CodeBlock.of("%L", p) }.joinToString(", ", "$call(", ")"))
    }
}
