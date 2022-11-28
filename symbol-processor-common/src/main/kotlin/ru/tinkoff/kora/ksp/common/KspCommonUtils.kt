package ru.tinkoff.kora.ksp.common

import com.google.devtools.ksp.KspExperimental
import com.google.devtools.ksp.getDeclaredFunctions
import com.google.devtools.ksp.processing.Resolver
import com.google.devtools.ksp.symbol.*
import com.google.devtools.ksp.visitor.KSEmptyVisitor
import com.squareup.kotlinpoet.AnnotationSpec
import com.squareup.kotlinpoet.ClassName
import com.squareup.kotlinpoet.CodeBlock
import com.squareup.kotlinpoet.ksp.toClassName
import ru.tinkoff.kora.common.Mapping
import ru.tinkoff.kora.common.naming.NameConverter
import ru.tinkoff.kora.ksp.common.AnnotationUtils.findAnnotation
import ru.tinkoff.kora.ksp.common.AnnotationUtils.findValue
import ru.tinkoff.kora.ksp.common.exception.ProcessingErrorException
import kotlin.reflect.KClass

object KspCommonUtils {
    fun KSAnnotated.findRepeatableAnnotation(annotationName: ClassName, containerName: ClassName): List<KSAnnotation> {
        val annotations = this.annotations
            .filter { it.shortName.asString() == annotationName.simpleName && it.annotationType.resolve().declaration.qualifiedName?.asString() == annotationName.canonicalName }
        val containeredAnnotations = this.annotations
            .filter { it.shortName.asString() == containerName.simpleName && it.annotationType.resolve().declaration.qualifiedName?.asString() == containerName.canonicalName }
            .flatMap { it.arguments.asSequence().filter { it.name?.asString() == "value" } }
            .flatMap { it.value as ArrayList<KSAnnotation> }

        return sequenceOf(annotations, containeredAnnotations).flatMap { it }.toList()
    }

    fun KSType.fixPlatformType(resolver: Resolver): KSType {
        val type = if (this.nullability == Nullability.PLATFORM) {
            this.makeNotNullable()
        } else {
            this
        }
        val fixMutability = type.toString().startsWith("(Mutable")
        if (this.arguments.isEmpty()) {
            if (fixMutability) {
                return type.immutableDeclaration(resolver).asType(listOf())
            } else {
                return type
            }
        }
        var changed = false
        val args = ArrayList<KSTypeArgument>(this.arguments.size)
        for (arg in this.arguments) {
            val argType = arg.type!!.resolve()
            val newArgType = argType.fixPlatformType(resolver)
            if (newArgType !== argType) {
                changed = true
                args.add(
                    resolver.getTypeArgument(
                        resolver.createKSTypeReferenceFromKSType(newArgType),
                        arg.variance
                    )
                )
            } else {
                args.add(arg)
            }
        }
        if (changed) {
            if (fixMutability) {
                return type.immutableDeclaration(resolver).asType(args)
            } else {
                return type.replace(args)
            }
        } else {
            if (fixMutability) {
                return type.immutableDeclaration(resolver).asType(listOf())
            } else {
                return type
            }
        }
    }


    private fun KSType.immutableDeclaration(resolver: Resolver): KSClassDeclaration {
        val declaration = this.declaration
        val immutableName = declaration.qualifiedName!!.asString().replaceFirst(".Mutable", ".")
        return resolver.getClassDeclarationByName(resolver.getKSNameFromString(immutableName))!!
    }
}


data class MappersData(val mapperClasses: List<KSType>, val tags: List<KSType>) {
    fun getMapping(type: KSType): MappingData? {
        if (mapperClasses.isEmpty() && tags.isEmpty()) {
            return null
        }
        for (mapperClass in mapperClasses) {
            if (type.isAssignableFrom(mapperClass)) {
                return MappingData(mapperClass, tags)
            }
        }
        if (tags.isEmpty()) {
            return null
        } else {
            return MappingData(null, tags)
        }
    }

    fun getMapping(type: ClassName): MappingData? {
        if (mapperClasses.isEmpty()) {
            return null
        }
        for (mapperClass in mapperClasses) {
            val declaration = mapperClass.declaration
            if (declaration !is KSClassDeclaration) {
                continue
            }
            if (declaration.doesImplement(type)) {
                return MappingData(mapperClass, tags)
            }
        }
        if (tags.isEmpty()) {
            return null
        } else {
            return MappingData(null, tags)
        }
    }

}

fun KSClassDeclaration.doesImplement(type: ClassName) = this.superTypes
    .any { (it.resolve().declaration as KSClassDeclaration).toClassName() == type }


data class MappingData(val mapper: KSType?, val tags: List<KSType>) {
    fun toTagAnnotation(): AnnotationSpec? {
        if (this.tags.isEmpty()) {
            return null
        }
        val tags = CodeBlock.builder()
        for (i in this.tags.indices) {
            if (i > 0) {
                tags.add(", ")
            }
            tags.add("%T::class", this.tags[i].toClassName())
        }
        return AnnotationSpec.builder(CommonClassNames.tag).addMember(tags.build()).build()
    }
}

@KspExperimental
fun KSAnnotated.parseMappingData(): MappersData {
    val tags = parseTagValue(this)
    val mappingsAnnotation = this.findAnnotation(Mapping.Mappings::class)
    if (mappingsAnnotation != null) {
        val mappings = mappingsAnnotation.findValue<List<KSAnnotation>>("value")!!
        val mappers = mappings.map { it.findValue<KSType>("value")!! }
        return MappersData(mappers, tags)
    }
    val mappers = parseAnnotationClassValue(this, Mapping::class.qualifiedName!!)
    return MappersData(mappers, tags)
}

fun <T> KSAnnotated.visitClass(visitor: (KSClassDeclaration) -> T) = this.accept(object : KSEmptyVisitor<Any?, T?>() {
    override fun defaultHandler(node: KSNode, data: Any?): T? = null

    override fun visitClassDeclaration(classDeclaration: KSClassDeclaration, data: Any?): T? {
        return visitor(classDeclaration)
    }
}, null)

fun <T> KSAnnotated.visitFunction(visitor: (KSFunctionDeclaration) -> T) = this.accept(object : KSEmptyVisitor<Any?, T?>() {
    override fun defaultHandler(node: KSNode, data: Any?): T? = null

    override fun visitFunctionDeclaration(function: KSFunctionDeclaration, data: Any?): T? {
        return visitor(function)
    }
}, null)

fun <T> KSAnnotated.visitFunctionArgument(visitor: (KSValueParameter) -> T) = this.accept(object : KSEmptyVisitor<Any?, T?>() {
    override fun defaultHandler(node: KSNode, data: Any?): T? = null

    override fun visitValueParameter(valueParameter: KSValueParameter, data: Any?): T? {
        return visitor(valueParameter)
    }
}, null)

@KspExperimental
fun parseAnnotationClassValue(target: KSAnnotated, annotationName: String): List<KSType> {
    val annotation = target.annotations.firstOrNull { it.annotationType.resolve().declaration.qualifiedName!!.asString() == annotationName }

    return annotation?.arguments?.filter {
        it.name!!.asString() == "value"
    }?.map {
        if (it.value is KSType) {
            listOf(it.value as KSType)
        } else it.value as ArrayList<KSType>
    }?.flatten() ?: listOf()
}


inline fun <reified T> parseAnnotationValue(target: KSAnnotation, name: String) = target.findValue<T>(name)

@KspExperimental
fun parseTagValue(target: KSAnnotated): List<KSType> {
    return parseAnnotationClassValue(target, CommonClassNames.tag.canonicalName)
}

fun findMethods(ksAnnotated: KSAnnotated, functionFilter: (KSFunctionDeclaration) -> Boolean): List<KSFunctionDeclaration> {
    if (ksAnnotated !is KSClassDeclaration) {
        return emptyList()
    }
    val result = ArrayList<KSFunctionDeclaration>()
    for (function in ksAnnotated.getDeclaredFunctions().toList()) {
        if (!functionFilter(function)) {
            continue
        }
        result.add(function)
    }
    return result
}

@KspExperimental
fun KSClassDeclaration.getNameConverter(): NameConverter? {
    val namingStrategy = this.findAnnotation(CommonClassNames.namingStrategy)
    return if (namingStrategy != null) {
        val namingStrategyClass = getNamingStrategyConverterClass(this)
        return if (namingStrategyClass != null) {
            try {
                val inst = namingStrategyClass.constructors.firstOrNull()?.call() as NameConverter?
                inst
            } catch (e: Exception) {
                throw ProcessingErrorException("Error on calling name converter constructor $this", this)
            }
        } else null
    } else null
}

@KspExperimental
fun getNamingStrategyConverterClass(declaration: KSClassDeclaration): KClass<*>? {
    val annotationValues = parseAnnotationClassValue(declaration, CommonClassNames.namingStrategy.canonicalName)
    if (annotationValues.isEmpty()) return null
    val type = annotationValues[0]
    if (type.declaration is KSClassDeclaration) {
        val className = (type.declaration as KSClassDeclaration).qualifiedName!!.asString()
        return try {
            Class.forName(className).kotlin
        } catch (e: ClassNotFoundException) {
            throw ProcessingErrorException("Class $className not found in classpath", declaration)
        }
    }
    return null
}

fun KSAnnotated.getOuterClassesAsPrefix(): String {
    val prefix = StringBuilder("$")
    var parent = this.parent
    while (parent != null && parent is KSClassDeclaration) {
        prefix.insert(1, parent.simpleName.asString() + "_")
        parent = parent.parent
    }
    return prefix.toString()
}

fun <T> measured(name: String, thunk: () -> T): T {
    val start = System.currentTimeMillis()
    try {
        return thunk().also {
            KoraSymbolProcessingEnv.logger.logging("$name processed in ${System.currentTimeMillis() - start}")
        }
    } catch (e: Exception) {
        KoraSymbolProcessingEnv.logger.exception(Exception("$name processed in ${System.currentTimeMillis() - start}", e))
        throw e
    }
}

fun KSClassDeclaration.isJavaRecord(): Boolean {
    return (this.origin == Origin.JAVA || this.origin == Origin.JAVA_LIB) && this.superTypes.any { it.resolve().declaration.qualifiedName?.asString() == "java.lang.Record" }
}
