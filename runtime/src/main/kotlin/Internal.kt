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

@file:Suppress("NOTHING_TO_INLINE", "PLATFORM_CLASS_MAPPED_TO_KOTLIN")
@file:OptIn(ExperimentalContracts::class)

package com.meowool.mmkv.ktx

import kotlinx.coroutines.coroutineScope
import kotlinx.coroutines.flow.FlowCollector
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.distinctUntilChanged
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.flow.stateIn
import java.util.Set
import kotlin.contracts.ExperimentalContracts
import kotlin.contracts.contract

internal const val defaultBool = false
internal const val defaultInt = -1
internal const val defaultLong = -1L
internal const val defaultFloat = Float.NaN
internal const val defaultDouble = Double.NaN
internal val defaultBytes: ByteArray? inline get() = null
internal val defaultString: String? inline get() = null
internal val defaultStringSet: Set<String>? inline get() = null

internal inline fun isDefault(value: Boolean) = value == defaultBool
internal inline fun isDefault(value: Int) = value == defaultInt
internal inline fun isDefault(value: Long) = value == defaultLong
internal inline fun isDefault(value: Float) = value.isNaN()
internal inline fun isDefault(value: Double) = value.isNaN()

internal inline fun isDefault(value: ByteArray?): Boolean {
  contract { returns(false) implies (value != null) }
  return value == null
}

internal inline fun isDefault(value: String?): Boolean {
  contract { returns(false) implies (value != null) }
  return value == null
}

internal inline fun isDefault(value: Set<String>?): Boolean {
  contract { returns(false) implies (value != null) }
  return value == null
}

/**
 * A lightweight operator that maps a [StateFlow] to another type of [StateFlow].
 *
 * [Fork source](https://github.com/Kotlin/kotlinx.coroutines/issues/2631#issuecomment-870565860)
 */
internal fun <T, R> StateFlow<T>.mapState(transform: (T) -> R): StateFlow<R> {
  val raw = this
  val flow = this.map { transform(it) }
  return object : StateFlow<R> {
    override val value: R get() = transform(raw.value)
    override val replayCache: List<R> get() = listOf(value)
    override suspend fun collect(collector: FlowCollector<R>): Nothing {
      // Does not produce the same value in a raw, so respect "distinct until changed emissions"
      coroutineScope { flow.distinctUntilChanged().stateIn(this).collect(collector) }
    }
  }
}
