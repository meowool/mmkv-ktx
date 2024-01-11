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

package com.meowool.mmkv.ktx.compiler.codegen

import com.google.devtools.ksp.symbol.KSClassDeclaration
import com.meowool.mmkv.ktx.compiler.Names
import com.squareup.kotlinpoet.AnnotationSpec
import com.squareup.kotlinpoet.FileSpec
import com.squareup.kotlinpoet.FunSpec
import com.squareup.kotlinpoet.KModifier
import com.squareup.kotlinpoet.PropertySpec
import com.squareup.kotlinpoet.TypeSpec

open class FactoryClass : CodegenStep() {
  override fun generate() {
    val className = context.factoryClassName
    val classBuilder = TypeSpec.interfaceBuilder(className)
    val preferences = context.preferences

    preferences.process {
      val propertySpec = PropertySpec.builder(
        name = it.preferencesName(),
        type = context.preferencesClassName(it),
      ).build()
      classBuilder.addProperty(propertySpec)
    }

    val classSpec = classBuilder.build()

    val factorySpec = FunSpec.builder(className.simpleName)
      .returns(className)
      .addModifiers(KModifier.INLINE)
      .addStatement("return %T()", context.factoryImplClassName)
      .build()

    // TODO: Add KDoc for generated symbols.
    FileSpec.builder(className)
      .addAnnotation(
        AnnotationSpec.builder(Suppress::class)
          .addMember("%S", "NOTHING_TO_INLINE")
          .build()
      )
      .addType(classSpec)
      .addFunction(factorySpec)
      .build()
      .write(preferences)
  }

  fun KSClassDeclaration.preferencesName(): String {
    val annotation = requireNotNull(findAnnotation(Names.Preferences))
    return annotation.findStringArgument("name") ?: simpleName.asString().lowercaseFirstChar()
  }
}
