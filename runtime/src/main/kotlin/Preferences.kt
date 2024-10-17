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

import android.os.Parcelable

/**
 * Marks a data class as a mapping of preferences in the native MMKV instance.
 *
 * Each constructor property of this class corresponds to the name and default value of the
 * key-value item in the preferences, **so each property must declare a default value**.
 *
 * ## Supported types
 *
 * - [Boolean]
 * - [Int]
 * - [Long]
 * - [Float]
 * - [Double]
 * - [ByteArray]
 * - [String]
 * - [Set]<[String]>
 * - [Parcelable]
 *   **Any class that implements the [Parcelable] interface also be supported**
 *
 * In addition to the types mentioned above, it is also possible to manually convert any other
 * type by creating an object marked with [TypeConverters].
 *
 * ## Example
 * ```
 * @Preferences
 * data class GeneralSettings(
 *   val checkUpdates: Boolean = true,
 * )
 * ```
 */
@Target(AnnotationTarget.CLASS)
@Retention(AnnotationRetention.SOURCE)
annotation class Preferences(
  /**
   * The unique ID of the native MMKV instance.
   *
   * If not set, defaults to the class name.
   */
  val id: String = "",

  /**
   * The name in the generated `PreferencesFactory`.
   *
   * If not set, defaults to the class name starting with a lowercase letter.
   * For example: `GeneralSettings` -> `generalSettings`.
   */
  val name: String = "",

  /**
   * The expiration time (in milliseconds) for the preferences.
   * If set to a value greater than -1, the preferences will expire after the specified time.
   * If set to -1 (the default value), the preferences will never expire.
   */
  val expires: Int = -1,

  /**
   * The encryption key to use for the preferences.
   * If set to a non-empty string, the preferences will be encrypted using the specified key.
   * If left empty (the default value), the preferences will not be encrypted.
   */
  val cryptKey: String = "",
)
