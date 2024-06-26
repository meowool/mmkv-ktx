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
import com.meowool.mmkv.ktx.tests.utils.Json

@TypeConverters
class ConstructedConverters(private val json: Json) {
  fun Map<String, String>.toJson(): String = json.encodeToJson(this)
  fun String.toMap(): Map<String, String> = json.decodeFromJson(this)
  fun Map<String, String>?.toNullableJson(): String? = this?.toJson()
  fun String?.toNullableMap(): Map<String, String>? = this?.toMap()
}
