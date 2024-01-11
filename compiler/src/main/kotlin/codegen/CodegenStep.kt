/*
 * Copyright (C) 2024 Meowool <https://github.com/meowool/mmkv-ktx/graphs/contributors>
 *
 * This file is part of the MMKV-KTX project <https://github.com/meowool/mmkv-ktx>.
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *     http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

@file:Suppress("MemberVisibilityCanBePrivate")

package com.meowool.mmkv.ktx.compiler.codegen

import com.google.devtools.ksp.findActualType
import com.google.devtools.ksp.processing.CodeGenerator
import com.google.devtools.ksp.processing.KSPLogger
import com.google.devtools.ksp.symbol.KSAnnotated
import com.google.devtools.ksp.symbol.KSAnnotation
import com.google.devtools.ksp.symbol.KSClassDeclaration
import com.google.devtools.ksp.symbol.KSDeclaration
import com.google.devtools.ksp.symbol.KSFile
import com.google.devtools.ksp.symbol.KSFunctionDeclaration
import com.google.devtools.ksp.symbol.KSType
import com.google.devtools.ksp.symbol.KSTypeAlias
import com.google.devtools.ksp.symbol.KSTypeReference
import com.squareup.kotlinpoet.ClassName
import com.squareup.kotlinpoet.FileSpec
import com.squareup.kotlinpoet.MemberName
import com.squareup.kotlinpoet.ksp.kspDependencies
import com.squareup.kotlinpoet.ksp.originatingKSFiles
import java.util.Locale

abstract class CodegenStep {
  private var recordedExceptions = mutableSetOf<Throwable>()
  private val deferredNodes = mutableListOf<KSAnnotated>()

  protected lateinit var context: Context

  protected abstract fun generate()

  fun start(context: Context): List<KSAnnotated> {
    this.context = context
    // Every time we start a new round, we need to clear the exception nodes of the
    // previous round, because we don't need them anymore.
    deferredNodes.clear()
    generate()
    // We return the new deferred nodes so that they can be processed in the next round
    // of symbol processing.
    return deferredNodes
  }

  fun reportException() = recordedExceptions
    .distinctBy { it.message }
    .forEach { context.logger.exception(it) }

  open fun String.fixGeneratedCode() = this

  protected fun List<KSClassDeclaration>.process(generate: (KSClassDeclaration) -> Unit) = forEach {
    try {
      generate(it)
    } catch (e: Throwable) {
      // Record it if this symbol processing fails, so that it can be deferred to
      // the next round of symbol processing.
      recordedExceptions.add(e)
      deferredNodes.add(it)
    }
  }

  fun KSFunctionDeclaration.toMemberName() = MemberName(
    packageName = parentDeclaration!!.qualifiedName!!.asString(),
    simpleName = simpleName.asString()
  )

  fun KSDeclaration.logName() = qualifiedName?.asString() ?: simpleName.asString()

  fun KSTypeReference?.logName(): String = buildString {
    val type = this@logName?.resolve() ?: return@buildString
    append(type.declaration.simpleName.asString())
    if (type.arguments.isNotEmpty()) {
      append("<")
      append(type.arguments.joinToString { it.type!!.logName() })
      append(">")
    }
    if (type.isMarkedNullable) append("?")
  }

  fun KSTypeReference?.matches(other: Any?): Boolean {
    if (this == null && other == null) return true
    if (this == null || other == null) return false
    val aType = resolve()
    val a = aType.declaration
    val b = when (other) {
      is ClassName -> return a.qualifiedName?.asString() == other.canonicalName
      is KSDeclaration -> other
      is KSTypeReference -> return matches(other.resolve())
      is KSType -> {
        if (other.arguments.size != aType.arguments.size) return false
        if (other.arguments.isNotEmpty()) {
          val aArgs = aType.arguments.map { it.type }
          val bArgs = other.arguments.map { it.type }
          if (aArgs.zip(bArgs).any { (a, b) -> !a.matches(b) }) return false
        }
        other.declaration
      }
      else -> return false
    }
    return a.qualifiedName?.asString() == b.qualifiedName?.asString()
  }

  fun KSType?.matchesNullable(other: KSType?): Boolean {
    if (this == null && other == null) return true
    if (this == null || other == null) return false
    if (this.isMarkedNullable != other.isMarkedNullable) return false
    return true
  }

  fun Sequence<KSTypeReference>.contains(className: ClassName) = any {
    it.resolve().declaration.qualifiedName?.asString() == className.canonicalName
  }

  fun KSType.findActualDeclaration() = when (val symbol = declaration) {
    is KSClassDeclaration -> symbol
    is KSTypeAlias -> symbol.findActualType()
    else -> {
      context.logger.warn("Cannot find actual declaration of the type", symbol)
      null
    }
  }

  fun KSAnnotated.findAnnotation(className: ClassName) = annotations.find {
    it.shortName.asString() == className.simpleName && it.annotationType.resolve()
      .declaration.qualifiedName?.asString() == className.canonicalName
  }

  fun KSAnnotation?.findArgument(name: String) = this?.arguments?.find {
    it.name?.asString() == name
  }

  fun KSAnnotation?.findStringArgument(name: String) =
    when (val value = findArgument(name)?.value as? String) {
      null -> null
      else -> value.takeIf { it.isNotEmpty() }
    }

  fun FileSpec.write(originatingDeclarations: Iterable<KSDeclaration>) =
    write(originatingDeclarations.mapNotNull { it.containingFile }.toSet())

  fun FileSpec.write(originatingDeclaration: KSDeclaration) =
    write(listOf(originatingDeclaration.containingFile!!))

  @JvmName("writeWithFiles")
  fun FileSpec.write(originatingKSFiles: Iterable<KSFile> = originatingKSFiles()) {
    val dependencies = kspDependencies(
      aggregating = true,
      // We need the source file of the converter anyway, because we always depend on them.
      originatingKSFiles = originatingKSFiles + context.typeConverters.mapNotNull { it.containingFile }
    )
    val source = this.toString().fixGeneratedCode().replace("`get`()", "get()")

    context.codeGenerator
      .createNewFile(dependencies, packageName, name)
      .bufferedWriter()
      .use { it.write(source) }
  }

  fun String.lowercaseFirstChar() = replaceFirstChar { it.lowercase(Locale.getDefault()) }

  fun String.uppercaseFirstChar() = replaceFirstChar { it.uppercase(Locale.getDefault()) }

  data class Context(
    val preferences: List<KSClassDeclaration>,
    val typeConverters: List<KSClassDeclaration>,
    val logger: KSPLogger,
    val codeGenerator: CodeGenerator,
    val packageName: String,
  ) {
    val factoryClassName = className("PreferencesFactory")
    val factoryImplClassName = className("PreferencesFactoryImpl")

    fun mutableClassName(raw: KSClassDeclaration) =
      className("Mutable" + raw.simpleName.asString())

    fun mutableImplClassName(raw: KSClassDeclaration) =
      className("Mutable" + raw.simpleName.asString() + "Impl")

    fun preferencesClassName(raw: KSClassDeclaration) =
      className(raw.simpleName.asString() + "Preferences")

    fun preferencesImplClassName(raw: KSClassDeclaration) =
      className(raw.simpleName.asString() + "PreferencesImpl")

    private fun className(name: String) = ClassName(packageName, name)
  }
}
