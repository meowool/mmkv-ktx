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

package com.meowool.mmkv.ktx.tests

import com.meowool.mmkv.ktx.TypeConverters
import java.nio.ByteBuffer
import java.util.Date

@TypeConverters
object Converters {
  fun Date.toLong(): Long = time
  fun Long.toDate(): Date = Date(this)
  fun Date?.toLongNullable(): Long = this?.time ?: -1
  fun Long.toDateNullable(): Date? = if (this == -1L) null else Date(this)

  fun List<Int>.toBytes(): ByteArray = ByteBuffer.allocate(size * 4).apply {
    forEach { putInt(it) }
  }.array()

  fun ByteArray.toInts(): List<Int> = ByteBuffer.wrap(this).let { buffer ->
    val list = mutableListOf<Int>()
    while (buffer.hasRemaining()) list.add(buffer.int)
    list
  }

  fun List<Int>?.toBytesNullable(): ByteArray? = this?.toBytes()
  fun ByteArray?.toIntsNullable(): List<Int>? = this?.toInts()
}
