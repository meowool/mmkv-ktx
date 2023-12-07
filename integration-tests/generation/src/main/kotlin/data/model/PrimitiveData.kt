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

@file:Suppress("ArrayInDataClass")

package com.meowool.mmkv.ktx.tests.data.model

import com.meowool.mmkv.ktx.Preferences
import kotlinx.parcelize.Parcelize
import android.os.Parcelable as AndroidParcelable

@Preferences
data class PrimitiveData(
  val enum: Enum? = Enum.First,
  val enumNullable: Enum? = null,
  val parcelable: Parcelable = Parcelable(""),
  val parcelableNullable: Parcelable? = null,
  val bool: Boolean = false,
  val boolNullable: Boolean? = null,
  val int: Int = 0,
  val intNullable: Int? = null,
  val long: Long = 0L,
  val longNullable: Long? = null,
  val string: String = "",
  val stringNullable: String? = null,
  val bytes: ByteArray = ByteArray(0),
  val bytesNullable: ByteArray? = null,
  val set: Set<String> = emptySet(),
  val setNullable: Set<String>? = null,
) {
  @Parcelize
  data class Parcelable(val value: String) : AndroidParcelable
  enum class Enum { First, Second, Third }
}
