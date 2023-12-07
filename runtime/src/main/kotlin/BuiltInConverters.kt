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

package com.meowool.mmkv.ktx

import java.nio.ByteBuffer

internal object BuiltInConverters {
  fun decodeNullableBoolean(value: Int): Boolean? = when (value) {
    1 -> true
    0 -> false
    else -> null
  }

  fun encodeNullableBoolean(boolean: Boolean?): Int = when (boolean) {
    true -> 1
    false -> 0
    else -> -1
  }

  fun decodeNullableInt(value: Long): Int? = when (value) {
    Long.MAX_VALUE -> null
    else -> value.toInt()
  }

  fun encodeNullableInt(int: Int?): Long = int?.toLong() ?: Long.MAX_VALUE

  fun decodeNullableLong(bytes: ByteArray?): Long? =
    bytes?.let { ByteBuffer.wrap(it).getLong() }

  fun encodeNullableLong(value: Long?): ByteArray? = value?.let {
    ByteBuffer
      .allocate(Long.SIZE_BYTES)
      .also { it.putLong(value) }
      .array()
  }

  fun decodeNullableFloat(bytes: ByteArray?): Float? =
    bytes?.let { ByteBuffer.wrap(it).getFloat() }

  fun encodeNullableFloat(value: Float?): ByteArray? = value?.let {
    ByteBuffer
      .allocate(Float.SIZE_BYTES)
      .also { it.putFloat(value) }
      .array()
  }

  fun decodeNullableDouble(bytes: ByteArray?): Double? =
    bytes?.let { ByteBuffer.wrap(it).getDouble() }

  fun encodeNullableDouble(value: Double?): ByteArray? = value?.let {
    ByteBuffer
      .allocate(Double.SIZE_BYTES)
      .also { it.putDouble(value) }
      .array()
  }
}
