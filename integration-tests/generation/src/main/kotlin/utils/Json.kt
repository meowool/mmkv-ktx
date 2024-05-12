/*
 * Copyright (C) 2024 Meowool <https://github.com/meowool/mmkv-ktx/graphs/contributors>
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

package com.meowool.mmkv.ktx.tests.utils

class Json {
  fun encodeToJson(map: Map<String, String>): String = buildString {
    append("{")
    map.entries.forEachIndexed { index, (key, value) ->
      if (index > 0) append(",")
      append("\"$key\":\"$value\"")
    }
    append("}")
  }

  fun decodeFromJson(json: String): Map<String, String> {
    val map = mutableMapOf<String, String>()
    val jsonObject = json.substring(1, json.lastIndex)
    val jsonPairs = jsonObject.split(",")
    jsonPairs.forEach { jsonPair ->
      val (key, value) = jsonPair.split(":")
      map[key.substring(1, key.lastIndex)] = value.substring(1, value.lastIndex)
    }
    return map
  }
}
