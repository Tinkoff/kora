package ru.tinkoff.kora.database.symbol.processor.r2dbc

import com.squareup.kotlinpoet.FunSpec
import com.squareup.kotlinpoet.ksp.toTypeName
import ru.tinkoff.kora.database.symbol.processor.QueryWithParameters
import ru.tinkoff.kora.database.symbol.processor.model.QueryParameter
import ru.tinkoff.kora.ksp.common.FieldFactory
import ru.tinkoff.kora.ksp.common.KotlinPoetUtils.controlFlow
import ru.tinkoff.kora.ksp.common.parseMappingData

object R2dbcStatementSetterGenerator {
    fun generate(
        b: FunSpec.Builder,
        query: QueryWithParameters,
        parameters: List<QueryParameter>,
        batchParam: QueryParameter?,
        parameterMappers: FieldFactory
    ) {
        if (batchParam != null) {
            b.beginControlFlow("for (_batch_%L in %N)", batchParam.name, batchParam.name)
        }
        var sqlIndex = 0
        parameters.forEach { p ->
            var parameter = p
            if (parameter is QueryParameter.ConnectionParameter) {
                return@forEach
            }
            var parameterName = parameter.name
            if (parameter is QueryParameter.BatchParameter) {
                parameter = parameter.parameter
                parameterName = "_batch_${parameterName}"
            }
            if (parameter is QueryParameter.SimpleParameter) {
                val nativeType = R2dbcNativeTypes.findNativeType(parameter.type.toTypeName())
                val mapping = parameter.variable.parseMappingData().getMapping(R2dbcTypes.parameterColumnMapper)
                if (nativeType != null && mapping == null) {
                    if (parameter.type.isMarkedNullable) {
                        b.controlFlow("if (%L != null)", parameterName) {
                            addCode(nativeType.bind("_stmt", parameterName, sqlIndex)).addCode("\n")
                            nextControlFlow("else")
                            addCode(nativeType.bindNull("_stmt", sqlIndex)).addCode("\n")
                        }
                    } else {
                        b.addCode(nativeType.bind("_stmt", parameterName, sqlIndex)).addCode("\n")
                    }
                } else if (mapping?.mapper != null) {
                    val mapper = parameterMappers.get(mapping.mapper!!, mapping.tags)
                    b.addCode("%N.apply(_stmt, %L, %N)\n", mapper, sqlIndex, parameterName)
                } else {
                    val mapper = parameterMappers.get(R2dbcTypes.parameterColumnMapper, parameter.type, parameter.variable)
                    b.addCode("%N.apply(_stmt, %L, %N)\n", mapper, sqlIndex, parameterName)
                }
                sqlIndex++
                return@forEach
            }
            if (parameter is QueryParameter.EntityParameter) {
                for (field in parameter.entity.fields) {
                    val sqlParam = query.find(parameter.name + "." + field.property.simpleName.getShortName())
                    if (sqlParam?.sqlIndexes.isNullOrEmpty()) {
                        continue
                    }
                    val nativeType = R2dbcNativeTypes.findNativeType(field.type.toTypeName())
                    val fieldValue = if (parameter.type.isMarkedNullable) {
                        parameterName + "?." + field.property.simpleName.getShortName()
                    } else {
                        parameterName + "." + field.property.simpleName.getShortName()
                    }
                    val mapping = field.mapping.getMapping(R2dbcTypes.parameterColumnMapper)
                    if (nativeType != null && mapping == null) {
                        if (parameter.type.isMarkedNullable || field.type.isMarkedNullable) {
                            b.beginControlFlow("if (%L != null)", fieldValue)
                        }
                        b.addCode(nativeType.bind("_stmt", fieldValue, sqlIndex)).addCode("\n")
                        if (parameter.type.isMarkedNullable || field.type.isMarkedNullable) {
                            b.nextControlFlow("else")
                            b.addCode(nativeType.bindNull("_stmt", sqlIndex)).addCode("\n")
                            b.endControlFlow()
                        }
                    } else if (mapping?.mapper != null) {
                        val mapper = parameterMappers.get(mapping.mapper!!, mapping.tags)
                        b.addStatement("%N.apply(_stmt, %L, %L)", mapper, sqlIndex, fieldValue)
                    } else {
                        val mapper = parameterMappers.get(R2dbcTypes.parameterColumnMapper, field.type, field.property)
                        b.addStatement("%N.apply(_stmt, %L, %L)", mapper, sqlIndex, fieldValue)
                    }
                    sqlIndex++
                }
            }
        }
        if (batchParam != null) {
            b.endControlFlow()
        }
    }
}
