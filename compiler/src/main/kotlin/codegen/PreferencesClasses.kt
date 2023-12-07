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
import com.meowool.mmkv.ktx.compiler.Names.StateFlow
import com.squareup.kotlinpoet.FileSpec
import com.squareup.kotlinpoet.FunSpec
import com.squareup.kotlinpoet.KModifier
import com.squareup.kotlinpoet.ParameterizedTypeName.Companion.parameterizedBy
import com.squareup.kotlinpoet.TypeSpec
import com.squareup.kotlinpoet.UNIT
import com.squareup.kotlinpoet.ksp.toClassName
import com.squareup.kotlinpoet.LambdaTypeName.Companion.get as lambdaType

class PreferencesClasses(override val context: Context) : Codegen() {
  override fun generate() = context.preferences.forEach(::generatePreferences)

  private fun generatePreferences(preferences: KSClassDeclaration) {
    val className = context.preferencesClassName(preferences)

    val getSpec = FunSpec.builder("get")
      .addModifiers(KModifier.ABSTRACT)
      .returns(preferences.toClassName())
      .build()

    val mutableSpec = FunSpec.builder("mutable")
      .addModifiers(KModifier.ABSTRACT)
      .returns(context.mutableClassName(preferences))
      .build()

    val updateSpec = FunSpec.builder("update")
      .addModifiers(KModifier.ABSTRACT)
      .addParameter("mutable", context.mutableClassName(preferences))
      .build()

    val asStateFlowSpec = FunSpec.builder("asStateFlow")
      .addModifiers(KModifier.ABSTRACT)
      .returns(StateFlow.parameterizedBy(preferences.toClassName()))
      .build()

    val classSpec = TypeSpec.interfaceBuilder(className)
      .addFunction(getSpec)
      .addFunction(mutableSpec)
      .addFunction(updateSpec)
      .addFunction(asStateFlowSpec)
      .build()

    val updateInlineSpec = FunSpec.builder("update")
      .addModifiers(KModifier.INLINE)
      .addParameter(
        name = "block",
        type = lambdaType(
          parameters = arrayOf(context.mutableClassName(preferences)),
          returnType = UNIT
        )
      )
      .receiver(className)
      .addStatement("return update(this.mutable().apply(block))")
      .build()

    // TODO: Add KDoc for generated symbols.
    FileSpec.builder(className)
      .addType(classSpec)
      .addFunction(updateInlineSpec)
      .build()
      .write(preferences)
  }
}
