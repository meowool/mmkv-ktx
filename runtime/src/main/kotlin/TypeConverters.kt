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

/**
 * Marks a class or object as a collection of type converters. All public extension functions
 * in this class will be treated as a type converter.
 *
 * When encoding/decoding a type that is not natively supported by MMKV, it will try to use
 * these converters for conversion.
 *
 * ## Important
 *
 * The type encoded by the type converter must be one of the following primitive types
 * supported by native MMKV, otherwise it will fail:
 *
 * - [Boolean]
 * - [Int]
 * - [Long]
 * - [Float]
 * - [Double]
 * - [String]
 * - [ByteArray]
 * - [Set]<[String]>
 *
 * ## Example
 * ```
 * @TypeConverters
 * object Converters {
 *   // Encode
 *   fun Date?.toTimestamp(): Long = this?.time ?: 0
 *   // Decode
 *   fun Long.toDate(): Date? = if (this == 0L) null else Date(this)
 * }
 * ```
 */
@Target(AnnotationTarget.CLASS)
@Retention(AnnotationRetention.SOURCE)
annotation class TypeConverters
