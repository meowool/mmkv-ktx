/*
 * Copyright (C) 2023 Meowool <https://github.com/meowool/mmkv-ktx/graphs/contributors>
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

package com.meowool.mmkv.ktx.compiler.codegen

import com.google.devtools.ksp.symbol.KSClassDeclaration
import com.google.devtools.ksp.symbol.KSValueParameter
import com.squareup.kotlinpoet.FileSpec
import com.squareup.kotlinpoet.FunSpec
import com.squareup.kotlinpoet.KModifier
import com.squareup.kotlinpoet.PropertySpec
import com.squareup.kotlinpoet.TypeSpec
import com.squareup.kotlinpoet.ksp.toClassName
import com.squareup.kotlinpoet.ksp.toTypeName

class MutableClasses(override val context: Context) : Codegen() {
  override fun generate() = context.preferences.forEach(::generateMutableClass)

  private fun generateMutableClass(preferences: KSClassDeclaration) {
    val className = context.mutableClassName(preferences)
    val constructor = requireNotNull(preferences.primaryConstructor) {
      "@Preferences class must have a primary constructor."
    }
    val properties = constructor.parameters

    val conversionSpec = FunSpec.builder("toImmutable")
      .addModifiers(KModifier.ABSTRACT)
      .returns(preferences.toClassName())
      .build()

    val classSpec = TypeSpec.interfaceBuilder(className)
      .addMappedProperties(properties)
      .addFunction(conversionSpec)
      .build()

    // TODO: Add KDoc for generated symbols.
    FileSpec.builder(className)
      .addType(classSpec)
      .build()
      .write(preferences)
  }

  private fun TypeSpec.Builder.addMappedProperties(raw: List<KSValueParameter>) = apply {
    raw.mapProperties().forEach { addProperty(it) }
  }

  private fun List<KSValueParameter>.mapProperties() = map {
    PropertySpec.builder(it.name!!.asString(), it.type.toTypeName())
      .mutable()
      .build()
  }
}
