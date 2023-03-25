package ru.tinkoff.kora.database.symbol.processor.vertx

import com.google.devtools.ksp.processing.KSPLogger
import com.google.devtools.ksp.processing.Resolver
import com.google.devtools.ksp.symbol.KSClassDeclaration
import com.google.devtools.ksp.symbol.KSFunction
import com.google.devtools.ksp.symbol.KSFunctionDeclaration
import com.squareup.kotlinpoet.AnnotationSpec
import com.squareup.kotlinpoet.ClassName
import com.squareup.kotlinpoet.CodeBlock
import com.squareup.kotlinpoet.FunSpec
import com.squareup.kotlinpoet.KModifier
import com.squareup.kotlinpoet.MemberName
import com.squareup.kotlinpoet.ParameterSpec
import com.squareup.kotlinpoet.ParameterizedTypeName.Companion.parameterizedBy
import com.squareup.kotlinpoet.PropertySpec
import com.squareup.kotlinpoet.TypeSpec
import com.squareup.kotlinpoet.ksp.toTypeName
import ru.tinkoff.kora.database.symbol.processor.DbUtils
import ru.tinkoff.kora.database.symbol.processor.DbUtils.addMapper
import ru.tinkoff.kora.database.symbol.processor.DbUtils.findQueryMethods
import ru.tinkoff.kora.database.symbol.processor.DbUtils.parseExecutorTag
import ru.tinkoff.kora.database.symbol.processor.DbUtils.queryMethodBuilder
import ru.tinkoff.kora.database.symbol.processor.DbUtils.resultMapperName
import ru.tinkoff.kora.database.symbol.processor.DbUtils.updateCount
import ru.tinkoff.kora.database.symbol.processor.Mapper
import ru.tinkoff.kora.database.symbol.processor.QueryWithParameters
import ru.tinkoff.kora.database.symbol.processor.RepositoryGenerator
import ru.tinkoff.kora.database.symbol.processor.model.QueryParameter
import ru.tinkoff.kora.database.symbol.processor.model.QueryParameterParser
import ru.tinkoff.kora.ksp.common.AnnotationUtils.findAnnotation
import ru.tinkoff.kora.ksp.common.AnnotationUtils.findValue
import ru.tinkoff.kora.ksp.common.CommonClassNames
import ru.tinkoff.kora.ksp.common.CommonClassNames.isFlow
import ru.tinkoff.kora.ksp.common.CommonClassNames.isList
import ru.tinkoff.kora.ksp.common.FieldFactory
import ru.tinkoff.kora.ksp.common.FunctionUtils.isFlow
import ru.tinkoff.kora.ksp.common.FunctionUtils.isSuspend
import ru.tinkoff.kora.ksp.common.parseMappingData

class VertxCoroutineBasedRepositoryGenerator(private val resolver: Resolver, private val kspLogger: KSPLogger) : RepositoryGenerator {
    private val runBlocking = MemberName("kotlinx.coroutines", "runBlocking")
    private val repositoryInterface = resolver.getClassDeclarationByName(resolver.getKSNameFromString(VertxTypes.Coroutines.repository.canonicalName))?.asStarProjectedType()
    override fun repositoryInterface() = repositoryInterface

    override fun generate(repositoryType: KSClassDeclaration, typeBuilder: TypeSpec.Builder, constructorBuilder: FunSpec.Builder): TypeSpec {
        this.enrichWithExecutor(repositoryType, typeBuilder, constructorBuilder)
        val repositoryResolvedType = repositoryType.asStarProjectedType()
        val resultMappers = FieldFactory(typeBuilder, constructorBuilder, "_result_mapper_")
        val parameterMappers = FieldFactory(typeBuilder, constructorBuilder, "_parameter_mapper_")
        for (method in repositoryType.findQueryMethods()) {
            val methodType = method.asMemberOf(repositoryResolvedType)
            val parameters = QueryParameterParser.parse(listOf(VertxTypes.sqlConnection, VertxTypes.sqlClient), method, methodType)
            val queryAnnotation = method.findAnnotation(DbUtils.queryAnnotation)!!
            val queryString = queryAnnotation.findValue<String>("value")!!
            val query = QueryWithParameters.parse(queryString, parameters)
            val resultMapperName = this.parseResultMapper(method, parameters, methodType)?.let { resultMappers.addMapper(it) }
            DbUtils.parseParameterMappers(method, parameters, query, VertxTypes.parameterColumnMapper) { VertxNativeTypes.findNativeType(it.toTypeName()) != null }
                .forEach { parameterMappers.addMapper(it) }
            val methodSpec = this.generate(method, methodType, query, parameters, resultMapperName, parameterMappers)
            typeBuilder.addFunction(methodSpec)
        }

        return typeBuilder
            .addAnnotation(
                AnnotationSpec.builder(ClassName("kotlin", "OptIn"))
                    .addMember("%T::class", ClassName("kotlinx.coroutines", "ExperimentalCoroutinesApi"))
                    .build()
            )
            .primaryConstructor(constructorBuilder.build())
            .build()
    }

    private fun generate(funDeclaration: KSFunctionDeclaration, function: KSFunction, query: QueryWithParameters, parameters: List<QueryParameter>, resultMapperName: String?, parameterMappers: FieldFactory): FunSpec {
        var sql = query.rawQuery
        query.parameters.indices.asSequence()
            .map { query.parameters[it].sqlParameterName to "$" + (it + 1) }
            .sortedByDescending { it.first.length }
            .forEach { sql = sql.replace(":" + it.first, it.second) }

        val b = funDeclaration.queryMethodBuilder(resolver)
        b.addCode("val _query = %T(\n  %S,\n  %S\n)\n", DbUtils.queryContext, query.rawQuery, sql)
        val batchParam = parameters.firstOrNull { it is QueryParameter.BatchParameter }
        val isSuspend = funDeclaration.isSuspend()
        val isFlow = funDeclaration.isFlow()
        ParametersToTupleBuilder.generate(b, query, funDeclaration, parameters, batchParam, parameterMappers)
        val connectionParameter = parameters.asSequence().filterIsInstance<QueryParameter.ConnectionParameter>().firstOrNull()?.variable?.name?.asString()

        if (isSuspend) {
            b.addCode("return ")
        } else {
            b.addCode("return %M {\n", runBlocking)
        }

        if (batchParam != null) {
            if (connectionParameter == null) {
                b.addCode("%T.awaitBatch(this.vertxConnectionFactory, _query, _batchParams)", VertxTypes.Coroutines.repositoryHelper)
            } else {
                b.addCode("%T.awaitBatch(%N, this.vertxConnectionFactory.telemetry(), _query, _batchParams)", VertxTypes.Coroutines.repositoryHelper, connectionParameter)
            }
            if (function.returnType == resolver.builtIns.unitType) {
                b.addCode("  \n.let {}")
            }
        } else if (isFlow) {
            if (connectionParameter == null) {
                b.addCode("%T.flow(this.vertxConnectionFactory, _query, _tuple, %N)", VertxTypes.Coroutines.repositoryHelper, resultMapperName)
            } else {
                b.addCode("%T.flow(%N, this.vertxConnectionFactory.telemetry(), _query, _tuple, %N)", VertxTypes.Coroutines.repositoryHelper, connectionParameter, resultMapperName)
            }
        } else {
            if (function.returnType == resolver.builtIns.unitType) {
                if (connectionParameter == null) {
                    b.addCode("%T.await(this.vertxConnectionFactory, _query, _tuple)", VertxTypes.Coroutines.repositoryHelper)
                } else {
                    b.addCode("%T.await(%N, this.vertxConnectionFactory.telemetry(), _query, _tuple)", VertxTypes.Coroutines.repositoryHelper, connectionParameter)
                }
            } else {
                if (connectionParameter == null) {
                    b.addCode("%T.awaitSingleOrNull(this.vertxConnectionFactory, _query, _tuple", VertxTypes.Coroutines.repositoryHelper)
                } else {
                    b.addCode("%T.awaitSingleOrNull(%N, this.vertxConnectionFactory.telemetry(), _query, _tuple", VertxTypes.Coroutines.repositoryHelper, connectionParameter)
                }
                if (function.returnType?.toTypeName() == updateCount) {
                    b.addCode(") { %T.extractUpdateCount(it) }", VertxTypes.rowSetMapper)
                } else {
                    b.addCode(", %N)", resultMapperName)
                }
                if (function.returnType?.isMarkedNullable == false) {
                    b.addCode("!!")
                }
            }
        }
        b.addCode("\n")
        if (!isSuspend) {
            b.addCode(" }\n")
        }
        return b.build()
    }

    private fun parseResultMapper(method: KSFunctionDeclaration, parameters: List<QueryParameter>, methodType: KSFunction): Mapper? {
        for (parameter in parameters) {
            if (parameter is QueryParameter.BatchParameter) {
                return null
            }
        }
        val returnType = methodType.returnType!!
        val mapperName = method.resultMapperName()
        val mappings = method.parseMappingData()
        val resultSetMapper = mappings.getMapping(VertxTypes.rowSetMapper)
        val rowMapper = mappings.getMapping(VertxTypes.rowMapper)
        if (returnType.isFlow()) {
            val flowParam = returnType.arguments[0]
            val returnTypeName = flowParam.toTypeName().copy(false)
            val mapperType = VertxTypes.rowMapper.parameterizedBy(returnTypeName)
            if (rowMapper != null) {
                return Mapper(rowMapper, mapperType, mapperName)
            }
            return Mapper(mapperType, mapperName)
        }
        val mapperType = VertxTypes.rowSetMapper.parameterizedBy(returnType.toTypeName())
        if (resultSetMapper != null) {
            return Mapper(resultSetMapper, mapperType, mapperName)
        }
        if (rowMapper != null) {
            if (returnType.isList()) {
                return Mapper(rowMapper, mapperType, mapperName) {
                    CodeBlock.of("%T.listRowSetMapper(%L)", VertxTypes.rowSetMapper, it)
                }
            } else {
                return Mapper(rowMapper, mapperType, mapperName) {
                    CodeBlock.of("%T.singleRowSetMapper(%L)", VertxTypes.rowSetMapper, it)
                }
            }
        }
        if (returnType == resolver.builtIns.unitType) {
            return null
        }
        if (returnType.toTypeName() == updateCount) {
            return null
        }
        return Mapper(mapperType, mapperName)
    }

    private fun enrichWithExecutor(repositoryElement: KSClassDeclaration, builder: TypeSpec.Builder, constructorBuilder: FunSpec.Builder) {
        builder.addSuperinterface(VertxTypes.Coroutines.repository)

        builder.addProperty(
            PropertySpec.builder("vertxConnectionFactory", VertxTypes.Coroutines.connectionFactory, KModifier.OVERRIDE)
                .build()
        )
        val executorTag = repositoryElement.parseExecutorTag()
        if (executorTag != null) {
            constructorBuilder.addParameter(
                ParameterSpec.builder("_vertxConnectionFactory", VertxTypes.Coroutines.connectionFactory).addAnnotation(
                    AnnotationSpec.builder(CommonClassNames.tag).addMember("value = %L", executorTag).build()
                ).build()
            )
        } else {
            constructorBuilder.addParameter("_vertxConnectionFactory", VertxTypes.Coroutines.connectionFactory)
        }
        constructorBuilder.addStatement("this.vertxConnectionFactory = _vertxConnectionFactory")
    }
}
