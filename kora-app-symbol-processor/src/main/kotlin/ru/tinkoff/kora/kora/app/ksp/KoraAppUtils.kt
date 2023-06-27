package ru.tinkoff.kora.kora.app.ksp

import com.google.devtools.ksp.*
import com.google.devtools.ksp.processing.Resolver
import com.google.devtools.ksp.symbol.*
import com.squareup.kotlinpoet.*
import com.squareup.kotlinpoet.ksp.addOriginatingKSFile
import com.squareup.kotlinpoet.ksp.toClassName
import com.squareup.kotlinpoet.ksp.toTypeName
import com.squareup.kotlinpoet.ksp.toTypeVariableName
import ru.tinkoff.kora.common.AopAnnotation
import ru.tinkoff.kora.ksp.common.findMethods


object KoraAppUtils {
    fun KSClassDeclaration.extendsKeepAop(resolver: Resolver, newName: String) = extendsKeepAop(this, resolver, newName)
}


fun extendsKeepAop(type: KSClassDeclaration, resolver: Resolver, newName: String): TypeSpec.Builder {
    val b: TypeSpec.Builder = TypeSpec.classBuilder(newName)
        .addOriginatingKSFile(type.containingFile!!)
    if (type.classKind == ClassKind.INTERFACE) {
        b.addSuperinterface(type.toClassName())
    } else {
        b.superclass(type.toClassName())
    }
    for (annotationMirror in type.annotations) {
        if (isAopAnnotation(resolver, annotationMirror)) {
            b.addModifiers(KModifier.OPEN)
            b.addAnnotation(annotationMirror.annotationType.resolve().toClassName())
            b.addModifiers(KModifier.OPEN)
        }
    }
    return b
}

fun overridingKeepAop(funDeclaration: KSFunctionDeclaration, resolver: Resolver): FunSpec.Builder {
    val funBuilder = FunSpec.builder(funDeclaration.simpleName.asString())
    if (funDeclaration.modifiers.contains(Modifier.SUSPEND)) {
        funBuilder.addModifiers(KModifier.SUSPEND)
    }
    if (funDeclaration.modifiers.contains(Modifier.PROTECTED)) {
        funBuilder.addModifiers(KModifier.PROTECTED)
    }
    if (funDeclaration.modifiers.contains(Modifier.PUBLIC)) {
        funBuilder.addModifiers(KModifier.PUBLIC)
    }

    for (typeParameter in funDeclaration.typeParameters) {
        funBuilder.addTypeVariable(typeParameter.toTypeVariableName())
    }
    funBuilder.addModifiers(KModifier.OVERRIDE)
    for (annotation in funDeclaration.annotations) {
        if (isAopAnnotation(resolver, annotation)) {
            funBuilder.addAnnotation(AnnotationSpec.builder(annotation.annotationType.resolve().toClassName()).build())
        }
    }
    val returnType = funDeclaration.returnType!!.resolve()
    if (returnType != resolver.builtIns.unitType) {
        funBuilder.returns(returnType.toTypeName())
    }
    for (i in funDeclaration.parameters.indices) {
        val parameter = funDeclaration.parameters[i]
        val parameterType = parameter.type
        val name = parameter.name!!.asString()
        val pb = ParameterSpec.builder(name, parameterType.toTypeName())
        if (parameter.isVararg) {
            pb.addModifiers(KModifier.VARARG)
        }
        for (annotation in parameter.annotations) {
            if (isAopAnnotation(resolver, annotation)) {
                pb.addAnnotation(AnnotationSpec.builder(annotation.annotationType.resolve().toClassName()).build())
            }
        }
        funBuilder.addParameter(pb.build())
    }

    return funBuilder
}

fun isClassExists(resolver: Resolver, fullClassName: String): Boolean {
    val declaration = resolver.getClassDeclarationByName(fullClassName)
    return declaration != null
}


fun hasAopAnnotations(resolver: Resolver, ksAnnotated: KSAnnotated): Boolean {
    if (hasAopAnnotation(resolver, ksAnnotated)) {
        return true
    }
    val methods = findMethods(ksAnnotated) { f ->
        f.isPublic() || f.isProtected()
    }
    for (method in methods) {
        if (hasAopAnnotation(resolver, method)) {
            return true
        }
        for (parameter in method.parameters) {
            if (hasAopAnnotation(resolver, parameter)) {
                return true
            }
        }
    }
    return false
}

private fun hasAopAnnotation(resolver: Resolver, e: KSAnnotated): Boolean {
    return e.annotations.any { isAopAnnotation(resolver, it) }
}

@OptIn(KspExperimental::class)
fun isAopAnnotation(resolver: Resolver, annotation: KSAnnotation): Boolean {
    return annotation.annotationType.resolve().declaration.isAnnotationPresent(AopAnnotation::class)
}

