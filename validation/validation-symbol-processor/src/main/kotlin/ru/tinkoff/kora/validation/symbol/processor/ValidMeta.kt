package ru.tinkoff.kora.validation.symbol.processor

import com.google.devtools.ksp.KspExperimental
import com.google.devtools.ksp.getClassDeclarationByName
import com.google.devtools.ksp.processing.Resolver
import com.google.devtools.ksp.symbol.*
import com.squareup.kotlinpoet.ClassName
import com.squareup.kotlinpoet.ParameterizedTypeName.Companion.parameterizedBy
import com.squareup.kotlinpoet.TypeName
import java.util.stream.Collectors

val CONTEXT_TYPE = ClassName.bestGuess("ru.tinkoff.kora.validation.common.ValidationContext")
val VALID_TYPE = ClassName.bestGuess("ru.tinkoff.kora.validation.common.annotation.Valid")
val VALIDATED_BY_TYPE = ClassName.bestGuess("ru.tinkoff.kora.validation.common.annotation.ValidatedBy")
val VALIDATOR_TYPE = ClassName.bestGuess("ru.tinkoff.kora.validation.common.Validator")
val VIOLATION_TYPE = ClassName.bestGuess("ru.tinkoff.kora.validation.common.Violation")
val EXCEPTION_TYPE = ClassName.bestGuess("ru.tinkoff.kora.validation.common.ViolationException")

data class ValidatorMeta(
    val source: Type,
    val sourceDeclaration: KSClassDeclaration,
    val validator: ValidatorType,
    val fields: List<Field>
)

data class Validated(val target: Type) {
    fun validator(): Type = VALIDATOR_TYPE.canonicalName.asType(listOf(target))
}

data class ValidatorType(val contract: Type, val implementation: Type)

data class Field(
    val type: Type,
    val name: String,
    val isDataClass: Boolean,
    val isNullable: Boolean,
    val constraint: List<Constraint>,
    val validates: List<Validated>
) {

    fun accessor(): String = name

    fun isNotNull(): Boolean = !isNullable
}

data class Constraint(val annotation: Type, val factory: Factory) {

    data class Factory(val type: Type, val parameters: Map<String, Any>) {

        fun validator(): Type = VALIDATOR_TYPE.canonicalName.asType(type.generic)
    }
}

data class Type(private val reference: KSTypeReference?, val packageName: String, val simpleName: String, val generic: List<Type>) {

    fun isNullable(): Boolean = reference?.resolve()?.isMarkedNullable ?: false

    fun canonicalName(): String = "$packageName.$simpleName"

    fun asKSType(resolver: Resolver): KSType {
        if (reference != null) {
            return reference.resolve()
        }

        val rootType = resolver.getClassDeclarationByName(canonicalName())
        val genericTypes = generic.asSequence()
            .map { it.asKSTypeArgument(resolver) }
            .toList()

        return rootType?.asType(genericTypes) ?: throw IllegalStateException("Can't extract declaration for: $this")
    }

    fun asKSTypeArgument(resolver: Resolver): KSTypeArgument {
        return if(reference != null) {
            resolver.getTypeArgument(reference, Variance.INVARIANT)
        } else if(generic.isEmpty()) {
            resolver.getTypeArgument(resolver.createKSTypeReferenceFromKSType(resolver.getClassDeclarationByName(canonicalName())!!.asStarProjectedType()), Variance.INVARIANT)
        } else {
            resolver.getTypeArgument(resolver.createKSTypeReferenceFromKSType(asKSType(resolver)), Variance.INVARIANT)
        }
    }

    @KspExperimental
    fun asPoetType(): TypeName = asPoetType(isNullable())

    @KspExperimental
    fun asPoetType(nullable: Boolean): TypeName {
        return if (generic.isEmpty()) {
            ClassName.bestGuess(canonicalName()).copy(nullable)
        } else {
            val genericPoetTypes = generic.asSequence()
                .map { t -> t.asPoetType() }
                .toList()
            ClassName.bestGuess(canonicalName()).parameterizedBy(genericPoetTypes).copy(nullable)
        }
    }

    override fun toString(): String {
        return if (generic.isEmpty()) {
            canonicalName()
        } else generic.stream()
            .map{ it.toString() }
            .collect(Collectors.joining(", ", canonicalName() + "<", ">"))
    }

    override fun equals(other: Any?): Boolean {
        if (this === other) return true
        if (other !is Type) return false

        if (packageName != other.packageName) return false
        if (simpleName != other.simpleName) return false
        if (generic != other.generic) return false
        return true
    }

    override fun hashCode(): Int {
        var result = packageName.hashCode()
        result = 31 * result + simpleName.hashCode()
        result = 31 * result + generic.hashCode()
        return result
    }
}

fun KSTypeReference.asType(): Type {
    val generic = if (this.element != null)
        this.element!!.typeArguments.asSequence()
            .filter { e -> e.type != null }
            .map { e -> e.type!!.asType() }
            .toList()
    else
        emptyList()

    val asType = this.resolve().declaration.qualifiedName!!.asString().asType()
    return Type(this, asType.packageName, asType.simpleName, generic)
}

fun String.asType(): Type = this.asType(emptyList())

fun String.asType(generic: List<Type>): Type {
    return Type(
        null,
        this.substring(0, this.lastIndexOf('.')),
        this.substring(this.lastIndexOf('.') + 1),
        generic
    )
}
