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

import android.app.Activity
import android.os.Bundle
import android.util.Log
import com.meowool.codegen.PreferencesFactory
import com.meowool.mmkv.ktx.tests.utils.Json
import com.tencent.mmkv.MMKV

class MainActivity : Activity() {
  override fun onCreate(savedInstanceState: Bundle?) {
    super.onCreate(savedInstanceState)
    MMKV.initialize(this)
    val preferences = PreferencesFactory(ConstructedConverters(Json()))
    val time = preferences.customData.get().date
    Log.d("MainActivity", "time = ${time.time}")
  }
}
