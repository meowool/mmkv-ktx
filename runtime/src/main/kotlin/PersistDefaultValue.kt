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
 * Marks a data property to indicate that its default value is persistent.
 *
 * This is useful for random values that only need to be initialized once, such as
 * generating an "ID" for an app on its first launch. In this case, marking the property
 * will return the same value as the initial value on each read until you manually
 * change it.
 *
 * ## Important
 *
 * This annotation can only be applied to the properties of the constructor of an class
 * marked with [Preferences].
 *
 * ## Example
 * ```
 * @Preferences
 * data class ApplicationData(
 *   @PersistDefaultValue
 *   val id: String = UUID.randomUUID().toString(),
 * )
 * ```
 */
@Target(AnnotationTarget.VALUE_PARAMETER)
@Retention(AnnotationRetention.SOURCE)
annotation class PersistDefaultValue
