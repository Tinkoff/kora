package ru.tinkoff.kora.validation.symbol.processor

import com.google.devtools.ksp.KspExperimental
import com.google.devtools.ksp.getAnnotationsByType
import com.google.devtools.ksp.isAnnotationPresent
import com.google.devtools.ksp.processing.Resolver
import com.google.devtools.ksp.processing.SymbolProcessorEnvironment
import com.google.devtools.ksp.symbol.*
import com.google.devtools.ksp.validate
import com.squareup.kotlinpoet.*
import com.squareup.kotlinpoet.ksp.KotlinPoetKspPreview
import com.squareup.kotlinpoet.ksp.writeTo
import ru.tinkoff.kora.common.Component
import ru.tinkoff.kora.ksp.common.BaseSymbolProcessor
import ru.tinkoff.kora.ksp.common.exception.ProcessingError
import ru.tinkoff.kora.ksp.common.exception.ProcessingErrorException
import ru.tinkoff.kora.ksp.common.visitClass
import ru.tinkoff.kora.validation.ValidationContext
import ru.tinkoff.kora.validation.Validator
import ru.tinkoff.kora.validation.Violation
import ru.tinkoff.kora.validation.annotation.Validated
import ru.tinkoff.kora.validation.annotation.ValidatedBy
import java.io.IOException
import javax.annotation.processing.Generated

@KspExperimental
@KotlinPoetKspPreview
class ValidationSymbolProcessor(private val environment: SymbolProcessorEnvironment) : BaseSymbolProcessor(environment) {

    data class ValidatorSpec(val meta: ValidatorMeta, val spec: TypeSpec, val parameterSpecs: List<ParameterSpec>)

    override fun processRound(resolver: Resolver): List<KSAnnotated> {
        val symbols = resolver.getSymbolsWithAnnotation(Validated::class.qualifiedName!!)
            .toList()

        try {
            val specs = symbols
                .filter { it.validate() }
                .mapNotNull { it.visitClass { clazz -> getValidatorMeta(clazz) } }
                .map { getValidatorSpecs(it) }
                .toList()

            for (validatorSpec in specs) {
                val fileSpec = FileSpec.builder(validatorSpec.meta.validator.implementation.packageName, validatorSpec.meta.validator.implementation.simpleName)
                    .addType(validatorSpec.spec)
                    .build()

                fileSpec.writeTo(codeGenerator = environment.codeGenerator, aggregating = false)
            }
        } catch (e: IOException) {
            throw ProcessingErrorException(ProcessingError(e.message.toString(), null))
        } catch (e: ValidationDeclarationException) {
            throw ProcessingErrorException(ProcessingError(e.message.toString(), e.declaration))
        }

        return symbols.filterNot { it.validate() }.toList()
    }

    private fun getValidatorSpecs(meta: ValidatorMeta): ValidatorSpec {
        val parameterSpecs = ArrayList<ParameterSpec>()
        val typeName = meta.validator.contract.asPoetType()
        val validatorSpecBuilder = TypeSpec.classBuilder(meta.validator.implementation.simpleName)
            .addSuperinterface(typeName)
            .addAnnotation(Component::class)
            .addAnnotation(
                AnnotationSpec.builder(Generated::class)
                    .addMember("%S", this.javaClass.canonicalName)
                    .build()
            )

        val constraintToFieldName = HashMap<Constraint.Factory, String>()
        val validatedToFieldName = HashMap<ValidatedTarget, String>()
        val contextBuilder = ArrayList<CodeBlock>()
        val constraintBuilder = ArrayList<CodeBlock>()
        for (i in meta.fields.indices) {
            val field = meta.fields[i]
            val contextField = "_context$i"
            contextBuilder.add(
                CodeBlock.of(
                    """
                var %L = context.addPath(%S)
                
                """.trimIndent(), contextField, field.name
                )
            )

            for (j in field.constraint.indices) {
                val constraint = field.constraint[j]
                val suffix = i.toString() + "_" + j
                val constraintField = constraintToFieldName.computeIfAbsent(constraint.factory) { "_constraint$suffix" }
                constraintBuilder.add(
                    CodeBlock.of(
                        """
                        _violations.addAll(%L.validate(value.%L, %L));
                        if(context.isFailFast && _violations.isNotEmpty()) {
                            return _violations;
                        }
                        
                        """.trimIndent(),
                        constraintField, field.accessor(), contextField
                    )
                )
            }

            for (j in field.validates.indices) {
                val validated = field.validates[j]
                val suffix = i.toString() + "_" + j
                val validatorField = validatedToFieldName.computeIfAbsent(validated) { "_validator$suffix" }
                if (field.isNotNull()) {
                    constraintBuilder.add(
                        CodeBlock.of(
                            """
                            
                        _violations.addAll(%L.validate(value.%L, %L));
                        if(context.isFailFast) {
                            return _violations;
                        }
                        
                        """.trimIndent(), validatorField, field.accessor(), contextField
                        )
                    )
                } else {
                    constraintBuilder.add(
                        CodeBlock.of(
                            """
                        if(value.%L != null) {
                            _violations.addAll(%L.validate(value.%L, %L));
                            if(context.isFailFast) {
                                return _violations;
                            }
                        }
                        
                        """.trimIndent(), field.accessor(), validatorField, field.accessor(), contextField
                        )
                    )
                }
            }
        }

        val constructorSpecBuilder = FunSpec.constructorBuilder().addModifiers(KModifier.PUBLIC)
        for (entry in constraintToFieldName) {
            val factory = entry.key
            val fieldName = entry.value
            val fieldMetaType = Validator::class.asType(factory.type.generic)
            val createParameters = factory.parameters.values.joinToString(", ") {
                if (it is String) {
                    CodeBlock.of("%S", it).toString()
                } else {
                    CodeBlock.of("%L", it).toString()
                }
            }

            validatorSpecBuilder.addProperty(
                PropertySpec.builder(
                    fieldName,
                    fieldMetaType.asPoetType(),
                    KModifier.PRIVATE
                ).build()
            )

            val parameterSpec = ParameterSpec.builder(fieldName, factory.type.asPoetType()).build()
            parameterSpecs.add(parameterSpec)
            constructorSpecBuilder
                .addParameter(parameterSpec)
                .addStatement("this.%L = %L.create(%L)", fieldName, fieldName, createParameters)
        }

        for (entry in validatedToFieldName) {
            val fieldName = entry.value
            val fieldType = entry.key.validator().asPoetType()
            PropertySpec.builder(fieldName, fieldType, KModifier.PRIVATE).build();
            validatorSpecBuilder.addProperty(PropertySpec.builder(fieldName, fieldType, KModifier.PRIVATE).build())
            val parameterSpec = ParameterSpec.builder(fieldName, fieldType).build()
            parameterSpecs.add(parameterSpec)
            constructorSpecBuilder
                .addParameter(parameterSpec)
                .addStatement("this.%L = %L", fieldName, fieldName)
        }

        val validateMethodSpecBuilder = FunSpec.builder("validate")
            .addModifiers(KModifier.OVERRIDE)
            .returns(Type(null, "kotlin.collections", "MutableList", listOf(Violation::class.asType())).asPoetType())
            .addParameter(ParameterSpec.builder("value", meta.source.asPoetType(true)).build())
            .addParameter(ParameterSpec.builder("context", ValidationContext::class.asType().asPoetType()).build())
            .addCode(
                CodeBlock.of(
                    """
                    if(value == null) {
                        return mutableListOf(context.violates("Input value is null"));
                    }
                    
                    val _violations = %T<%T>();
                    
                    """.trimIndent(), ArrayList::class, Violation::class
                )
            )

        contextBuilder.forEach { b -> validateMethodSpecBuilder.addCode(b) }
        constraintBuilder.forEach { b -> validateMethodSpecBuilder.addCode(b) }
        validateMethodSpecBuilder.addCode(CodeBlock.of("return _violations"))

        val typeSpec = validatorSpecBuilder
            .addFunction(constructorSpecBuilder.build())
            .addFunction(validateMethodSpecBuilder.build())
            .build()

        return ValidatorSpec(meta, typeSpec, parameterSpecs)
    }

    private fun getValidatorMeta(declaration: KSClassDeclaration): ValidatorMeta {
        if (declaration.classKind == ClassKind.INTERFACE || declaration.classKind == ClassKind.ENUM_CLASS) {
            throw ValidationDeclarationException("Validation can't be generated for: ${declaration.classKind}", declaration)
        }

        val elementFields = declaration.getAllProperties().toList()
        val fields = ArrayList<Field>()
        for (fieldProperty in elementFields) {
            val constraints = getConstraints(fieldProperty)
            val validateds = getValidated(fieldProperty)
            val isNullable = fieldProperty.type.resolve().isMarkedNullable
            if (constraints.isNotEmpty() || validateds.isNotEmpty()) {
                fields.add(
                    Field(
                        fieldProperty.type.asType(),
                        fieldProperty.simpleName.asString(),
                        declaration.modifiers.any { m -> m == Modifier.DATA },
                        isNullable,
                        constraints,
                        validateds
                    )
                )
            }
        }

        val source = declaration.qualifiedName!!.asString().asType()
        return ValidatorMeta(
            source,
            declaration,
            ValidatorType(
                Validator::class.asType(listOf(source)),
                Type(null, source.packageName, "\$Validator_" + source.simpleName, listOf(source)),
            ),
            fields
        )
    }

    private fun getConstraints(field: KSPropertyDeclaration): List<Constraint> {
        return field.annotations.asSequence()
            .filter { a -> a.annotationType.resolve().declaration.isAnnotationPresent(ValidatedBy::class) }
            .map { origin ->
                val validatedBy = origin.annotationType.resolve().declaration.getAnnotationsByType(ValidatedBy::class).first()
                val factory = validatedBy.value
                val parameters = origin.arguments.associate { a -> Pair(a.name!!.asString(), a.value!!) }

                Constraint(origin.annotationType.asType(), Constraint.Factory(factory.asType(listOf(field.type.asType())), parameters))
            }
            .toList()
    }

    private fun getValidated(field: KSPropertyDeclaration): List<ValidatedTarget> {
        return if (field.isAnnotationPresent(Validated::class))
            listOf(ValidatedTarget(field.type.asType()))
        else
            emptyList()
    }
}
