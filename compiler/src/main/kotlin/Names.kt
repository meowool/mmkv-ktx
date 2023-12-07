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

@file:Suppress("MemberVisibilityCanBePrivate")

package com.meowool.mmkv.ktx.compiler

import com.squareup.kotlinpoet.AnnotationSpec
import com.squareup.kotlinpoet.ClassName
import com.squareup.kotlinpoet.FileSpec

object Names {
  private const val PACKAGE = "com.meowool.mmkv.ktx"

  val Deprecated = ClassName("kotlin", "Deprecated")
  val DeprecationLevel = ClassName("kotlin", "DeprecationLevel")
  val StateFlow = ClassName("kotlinx.coroutines.flow", "StateFlow")
  val MutableStateFlow = ClassName(StateFlow.packageName, "MutableStateFlow")
  val MMKV = ClassName("com.tencent.mmkv", "MMKV")
  val Parcelable = ClassName("android.os", "Parcelable")

  val Preferences = ClassName(PACKAGE, "Preferences")
  val TypeConverters = ClassName(PACKAGE, "TypeConverters")
  val BuiltInConverters = ClassName(PACKAGE, "BuiltInConverters")

  fun FileSpec.Builder.addWriteOnlyImport(suppress: Boolean = true) = addInvisibleSuppress(suppress)
    .addImport(PACKAGE, "WRITE_ONLY")

  fun FileSpec.Builder.addChangesImport(suppress: Boolean = true) = addInvisibleSuppress(suppress)
    .addImport(PACKAGE, "UNCHANGED")
    .addImport(PACKAGE, "takeIfChanged")

  fun FileSpec.Builder.addInternalImport() = addWriteOnlyImport(false).addChangesImport(false)

  fun AnnotationSpec.Builder.addInvisibleSuppress() =
    addMember("%S", "INVISIBLE_REFERENCE").addMember("%S", "INVISIBLE_MEMBER")

  private fun FileSpec.Builder.addInvisibleSuppress(suppress: Boolean) = when (suppress) {
    true -> addAnnotation(
      AnnotationSpec.builder(Suppress::class)
        .addInvisibleSuppress()
        .build()
    )
    false -> this
  }
}
