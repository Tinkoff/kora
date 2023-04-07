package ru.tinkoff.kora.json.ksp.writer

import com.fasterxml.jackson.core.io.SerializedString
import com.google.devtools.ksp.getConstructors
import com.google.devtools.ksp.processing.Resolver
import com.google.devtools.ksp.symbol.KSClassDeclaration
import com.squareup.kotlinpoet.*
import com.squareup.kotlinpoet.ParameterizedTypeName.Companion.parameterizedBy
import com.squareup.kotlinpoet.ksp.*
import ru.tinkoff.kora.json.ksp.JsonTypes
import ru.tinkoff.kora.json.ksp.KnownType.KnownTypesEnum
import ru.tinkoff.kora.json.ksp.KnownType.KnownTypesEnum.*
import ru.tinkoff.kora.json.ksp.discriminatorField
import ru.tinkoff.kora.json.ksp.discriminatorValue
import ru.tinkoff.kora.json.ksp.jsonWriterName
import ru.tinkoff.kora.ksp.common.KotlinPoetUtils.controlFlow
import ru.tinkoff.kora.ksp.common.KspCommonUtils.generated
import ru.tinkoff.kora.ksp.common.KspCommonUtils.toTypeName

class JsonWriterGenerator(
    private val resolver: Resolver
) {

    fun generate(meta: JsonClassWriterMeta): TypeSpec {
        val jsonClassDeclaration = meta.classDeclaration
        val typeParameterResolver = jsonClassDeclaration.typeParameters.toTypeParameterResolver()
        val typeName = meta.classDeclaration.toTypeName()
        val writerInterface = JsonTypes.jsonWriter.parameterizedBy(typeName)
        val typeBuilder = TypeSpec.classBuilder(meta.classDeclaration.jsonWriterName())
            .generated(JsonWriterGenerator::class)
        jsonClassDeclaration.containingFile?.let { typeBuilder.addOriginatingKSFile(it) }
        typeBuilder.addSuperinterface(writerInterface)

        meta.classDeclaration.typeParameters.forEach {
            typeBuilder.addTypeVariable(it.toTypeVariableName())
        }

        this.addWriters(typeBuilder, meta, typeParameterResolver)
        for (field in meta.fields) {
            typeBuilder.addProperty(
                PropertySpec.builder(this.jsonNameStaticName(field), SerializedString::class, KModifier.PRIVATE)
                    .initializer(CodeBlock.of("%T(%S)", SerializedString::class.java, field.jsonName))
                    .build()
            )
        }
        val functionBody = CodeBlock.builder()
        functionBody.controlFlow("if (_object == null)") {
            addStatement("_gen.writeNull()")
            addStatement("return")
        }
        functionBody.addStatement("_gen.writeStartObject(_object)")

        val discriminatorField = meta.classDeclaration.discriminatorField(resolver)
        if (discriminatorField != null) {
            val discriminatorValue = meta.classDeclaration.discriminatorValue()
            functionBody.addStatement("_gen.writeFieldName(%S)", discriminatorField)
            functionBody.addStatement("_gen.writeString(%S)", discriminatorValue)
        }
        for (field in meta.fields) {
            this.addWriteParam(functionBody, field, typeParameterResolver)
        }
        functionBody.addStatement("_gen.writeEndObject()")

        typeBuilder.addFunction(
            FunSpec.builder("write")
                .addModifiers(KModifier.PUBLIC, KModifier.OVERRIDE)
                .addParameter("_gen", JsonTypes.jsonGenerator)
                .addParameter("_object", typeName.copy(nullable = true))
                .addCode(functionBody.build())
                .build()
        )

        return typeBuilder.build()
    }

    private fun addWriters(typeBuilder: TypeSpec.Builder, classMeta: JsonClassWriterMeta, typeParameterResolver: TypeParameterResolver) {
        val constructor = FunSpec.constructorBuilder()
        for (field in classMeta.fields) {
            if (field.writer != null) {
                val fieldName: String = this.writerFieldName(field)
                val fieldType = field.writer.toTypeName(typeParameterResolver)
                val writerProp = PropertySpec.builder(fieldName, fieldType, KModifier.PRIVATE)
                val writerDeclaration = field.writer.declaration as KSClassDeclaration
                if (!writerDeclaration.modifiers.contains(com.google.devtools.ksp.symbol.Modifier.OPEN)) {
                    val constructors = writerDeclaration.getConstructors().toList()
                    if (constructors.size == 1) {
                        writerProp.initializer("%T()", field.writer.toTypeName(typeParameterResolver))
                        typeBuilder.addProperty(writerProp.build())
                        continue
                    }
                }
                typeBuilder.addProperty(writerProp.build())
                constructor.addParameter(fieldName, fieldType)
                constructor.addStatement("this.%L = %L", fieldName, fieldName)
            } else if (field.typeMeta is WriterFieldType.UnknownWriterFieldType) {
                val fieldName: String = this.writerFieldName(field)
                val fieldType = JsonTypes.jsonWriter.parameterizedBy(
                    field.typeMeta.type.toTypeName(typeParameterResolver).copy(nullable = false)
                )
                val writerField = PropertySpec.builder(fieldName, fieldType, KModifier.PRIVATE)
                typeBuilder.addProperty(writerField.build())
                constructor.addParameter(fieldName, fieldType)
                constructor.addStatement("this.%L = %L", fieldName, fieldName)
            }
        }
        typeBuilder.primaryConstructor(constructor.build())
    }

    private fun writerFieldName(field: JsonClassWriterMeta.FieldMeta): String {
        return field.accessor + "Writer"
    }

    private fun addWriteParam(function: CodeBlock.Builder, field: JsonClassWriterMeta.FieldMeta, typeParameterResolver: TypeParameterResolver) {
        function.add("_gen.writeFieldName(%L)\n", this.jsonNameStaticName(field))
        if (field.writer == null && field.typeMeta is WriterFieldType.KnownWriterFieldType) {
            function.add(this.writeKnownType(field.typeMeta.knownType, field.typeMeta.markedNullable, CodeBlock.of("_object.%L", field.accessor)))
        } else if (field.writer == null) {
            function.controlFlow("if (_object.%L == null)", field.accessor) {
                addStatement("_gen.writeNull()")
                nextControlFlow("else")
                addStatement("%L.write(_gen, _object.%L!!)", writerFieldName(field), field.accessor)
            }
        } else {
            function.addStatement("%L.write(_gen, _object.%L)", this.writerFieldName(field), field.accessor)
        }
    }

    private fun jsonNameStaticName(field: JsonClassWriterMeta.FieldMeta): String {
        return "_" + field.fieldSimpleName.asString() + "_optimized_field_name"
    }

    private fun writeKnownType(
        knownType: KnownTypesEnum,
        isMarkedNullable: Boolean,
        value: CodeBlock
    ): CodeBlock {
        val nullableCodeBlock = if (isMarkedNullable) CodeBlock.of("if (%L == null) _gen.writeNull() else ", value) else CodeBlock.of("")

        return when (knownType) {
            KnownTypesEnum.STRING -> CodeBlock.of("%L_gen.writeString(%L)\n", nullableCodeBlock, value)
            KnownTypesEnum.BOOLEAN -> CodeBlock.of("%L_gen.writeBoolean(%L)\n", nullableCodeBlock, value)
            INTEGER, BIG_INTEGER, BIG_DECIMAL, KnownTypesEnum.DOUBLE, KnownTypesEnum.FLOAT, KnownTypesEnum.LONG, KnownTypesEnum.SHORT -> CodeBlock.of(
                "%L_gen.writeNumber(%L)\n",
                nullableCodeBlock,
                value
            )

            BINARY -> CodeBlock.of("%L_gen.writeBinary(%L)\n", nullableCodeBlock, value)
            UUID -> CodeBlock.of("%L_gen.writeString(%L.toString())\n", nullableCodeBlock, value)
        }
    }
}
