package ru.tinkoff.kora.ksp.common

import com.google.devtools.ksp.symbol.KSAnnotated
import com.google.devtools.ksp.symbol.KSType
import com.squareup.kotlinpoet.AnnotationSpec
import com.squareup.kotlinpoet.CodeBlock

object TagUtils {
    val ignoreList = setOf("Component", "DefaultComponent")

    fun parseTagValue(target: KSAnnotated): Set<String> {
        for (annotation in target.annotations.filter { !ignoreList.contains(it.shortName.asString()) }) {
            val type = annotation.annotationType.resolve()
            if (type.declaration.qualifiedName!!.asString() == CommonClassNames.tag.canonicalName) {
                return AnnotationUtils.parseAnnotationValueWithoutDefaults<List<KSType>>(annotation, "value")!!
                    .asSequence()
                    .map { it.declaration.qualifiedName!!.asString() }
                    .toSet()
            }
            for (annotatedWith in type.declaration.annotations) {
                val type = annotatedWith.annotationType.resolve()
                if (type.declaration.qualifiedName!!.asString() == CommonClassNames.tag.canonicalName) {
                    return AnnotationUtils.parseAnnotationValueWithoutDefaults<List<KSType>>(annotatedWith, "value")!!
                        .asSequence()
                        .map { it.declaration.qualifiedName!!.asString() }
                        .toSet()
                }

            }
        }
        return setOf()
    }

    fun Collection<String>.toTagAnnotation(): AnnotationSpec {
        val codeBlock = CodeBlock.builder().add("value = [")
        forEachIndexed { i, type ->
            if (i > 0) {
                codeBlock.add(", ")
            }
            codeBlock.add("%L::class", type)
        }
        val value = codeBlock.add("]").build()
        return AnnotationSpec.builder(CommonClassNames.tag).addMember(value).build()
    }
}
