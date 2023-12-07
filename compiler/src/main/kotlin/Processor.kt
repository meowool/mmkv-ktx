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

package com.meowool.mmkv.ktx.compiler

import com.google.auto.service.AutoService
import com.google.devtools.ksp.processing.CodeGenerator
import com.google.devtools.ksp.processing.KSPLogger
import com.google.devtools.ksp.processing.Resolver
import com.google.devtools.ksp.processing.SymbolProcessor
import com.google.devtools.ksp.processing.SymbolProcessorEnvironment
import com.google.devtools.ksp.processing.SymbolProcessorProvider
import com.google.devtools.ksp.symbol.ClassKind.OBJECT
import com.google.devtools.ksp.symbol.KSAnnotated
import com.google.devtools.ksp.symbol.KSClassDeclaration
import com.google.devtools.ksp.symbol.Modifier
import com.meowool.mmkv.ktx.compiler.Names.Preferences
import com.meowool.mmkv.ktx.compiler.Names.TypeConverters
import com.meowool.mmkv.ktx.compiler.codegen.Codegen
import com.meowool.mmkv.ktx.compiler.codegen.FactoryClass
import com.meowool.mmkv.ktx.compiler.codegen.FactoryImplClass
import com.meowool.mmkv.ktx.compiler.codegen.MutableClasses
import com.meowool.mmkv.ktx.compiler.codegen.PreferencesClasses
import com.meowool.mmkv.ktx.compiler.codegen.PreferencesImplClasses

class Processor(
  private val logger: KSPLogger,
  private val codeGenerator: CodeGenerator,
  private val packageName: String,
) : SymbolProcessor {
  override fun process(resolver: Resolver): List<KSAnnotated> {
    val context = Codegen.Context(
      preferences = resolver.filterPreferences(),
      typeConverters = resolver.filterTypeConverters(),
      logger = logger,
      codeGenerator = codeGenerator,
      packageName = packageName,
    )

    if (context.preferences.isEmpty() && context.typeConverters.isEmpty()) {
      logger.info("No @Preferences or @TypeConverters found.")
      return emptyList()
    }

    arrayOf(
      MutableClasses(context),
      FactoryClass(context),
      PreferencesClasses(context),
      FactoryImplClass(context),
      PreferencesImplClasses(context),
    ).forEach(Codegen::start)

    return emptyList()
  }

  private fun Resolver.filterPreferences(): List<KSClassDeclaration> =
    getSymbolsWithAnnotation(Preferences.canonicalName)
      .filterIsInstance<KSClassDeclaration>()
      .onEach { it.checkPreferences() }
      .toList()

  private fun Resolver.filterTypeConverters(): List<KSClassDeclaration> =
    getSymbolsWithAnnotation(TypeConverters.canonicalName)
      .filterIsInstance<KSClassDeclaration>()
      .onEach { it.checkTypeConverters() }
      .toList()

  private fun KSClassDeclaration.checkPreferences() {
    require(Modifier.DATA in modifiers) {
      "Only data class can be annotated with @Preferences."
    }
    logger.logging("Found preferences", symbol = this)
  }

  private fun KSClassDeclaration.checkTypeConverters() {
    require(!isCompanionObject && classKind == OBJECT) {
      "Only object class can be annotated with @TypeConverters."
    }
    logger.logging("Found type converters", symbol = this)
  }

  @AutoService(SymbolProcessorProvider::class)
  class Provider : SymbolProcessorProvider {
    override fun create(environment: SymbolProcessorEnvironment) = Processor(
      logger = environment.logger,
      codeGenerator = environment.codeGenerator,
      packageName = requireNotNull(environment.options["mmkv.ktx.packageName"]) {
        "'mmkv.ktx.packageName' must be specified in the KSP arguments."
      }
    )
  }
}
