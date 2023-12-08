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
import com.squareup.kotlinpoet.MemberName

object Names {
  private const val PACKAGE = "com.meowool.mmkv.ktx"

  val StateFlow = ClassName("kotlinx.coroutines.flow", "StateFlow")
  val MutableStateFlow = ClassName(StateFlow.packageName, "MutableStateFlow")
  val MMKV = ClassName("com.tencent.mmkv", "MMKV")
  val Parcelable = ClassName("android.os", "Parcelable")

  val Preferences = ClassName(PACKAGE, "Preferences")
  val TypeConverters = ClassName(PACKAGE, "TypeConverters")
  val PersistDefaultValue = ClassName(PACKAGE, "PersistDefaultValue")
  val BuiltInConverters = ClassName(PACKAGE, "BuiltInConverters")

  val isDefault = MemberName(PACKAGE, "isDefault")
  val mapState = MemberName(PACKAGE, "mapState")
  fun defaultValue(primitive: String) = MemberName(PACKAGE, "default$primitive")

  fun AnnotationSpec.Builder.addInvisibleSuppress() =
    addMember("%S", "INVISIBLE_REFERENCE").addMember("%S", "INVISIBLE_MEMBER")
}
