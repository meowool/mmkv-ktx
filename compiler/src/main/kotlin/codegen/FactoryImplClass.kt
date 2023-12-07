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

import com.squareup.kotlinpoet.FileSpec
import com.squareup.kotlinpoet.FunSpec
import com.squareup.kotlinpoet.KModifier
import com.squareup.kotlinpoet.PropertySpec
import com.squareup.kotlinpoet.TypeSpec

class FactoryImplClass(context: Context) : FactoryClass(context) {
  override fun generate() {
    val className = context.factoryImplClassName
    val classBuilder = TypeSpec.classBuilder(className)
      .addModifiers(KModifier.INTERNAL)
      .addSuperinterface(context.factoryClassName)
      .addAnnotation(PublishedApi::class)
    val preferences = context.preferences.toList()

    preferences.forEach {
      val propertyName = it.preferencesName()
      val type = context.preferencesClassName(it)
      val typeNullable = type.copy(nullable = true)

      val innerPropertySpec = PropertySpec.builder(name = "_$propertyName", typeNullable)
        .initializer("null")
        .mutable()
        .addModifiers(KModifier.PRIVATE)
        .addAnnotation(Volatile::class)
        .build()

      val overridePropertySpec = PropertySpec.builder(propertyName, type)
        .addModifiers(KModifier.OVERRIDE)
        .getter(
          FunSpec.getterBuilder()
            .beginControlFlow("return synchronized(this)")
            .addStatement(
              "%N·?:·%T().also·{·%N·=·it·}",
              innerPropertySpec,
              context.preferencesImplClassName(it),
              innerPropertySpec,
            )
            .endControlFlow()
            .build()
        )
        .build()

      classBuilder.addProperty(innerPropertySpec).addProperty(overridePropertySpec)
    }

    FileSpec.builder(className)
      .addType(classBuilder.build())
      .build()
      .write(preferences)
  }
}
