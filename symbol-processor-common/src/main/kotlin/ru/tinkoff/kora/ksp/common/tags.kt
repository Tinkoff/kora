package ru.tinkoff.kora.ksp.common

import com.google.devtools.ksp.symbol.KSAnnotated
import com.google.devtools.ksp.symbol.KSType
import com.squareup.kotlinpoet.AnnotationSpec
import com.squareup.kotlinpoet.ksp.toTypeName

fun KSAnnotated.parseTags(): List<KSType> = parseTags(this)

fun List<KSType>.makeTagAnnotationSpec(): AnnotationSpec = AnnotationSpec.builder(CommonClassNames.tag).let { builder ->
    this.forEach { tag -> builder.addMember("%T::class", tag.toTypeName()) }
    builder.build()
}
