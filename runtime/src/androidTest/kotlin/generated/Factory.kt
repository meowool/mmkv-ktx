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

@file:Suppress("TestFunctionName", "NOTHING_TO_INLINE")

package com.meowool.mmkv.ktx.test.generated

import com.meowool.mmkv.ktx.test.model.GeneralSettings
import com.meowool.mmkv.ktx.test.model.ProfileData
import kotlinx.coroutines.flow.StateFlow

interface PreferencesFactory {
  val generalSettings: GeneralSettingsPreferences
  val profileData: ProfileDataPreferences
}

interface GeneralSettingsPreferences {
  fun get(): GeneralSettings
  fun mutable(): MutableGeneralSettings
  fun update(mutable: MutableGeneralSettings)
  fun asStateFlow(): StateFlow<GeneralSettings>
}

interface ProfileDataPreferences {
  fun get(): ProfileData
  fun mutable(): MutableProfileData
  fun update(mutable: MutableProfileData)
  fun asStateFlow(): StateFlow<ProfileData>
}

inline fun PreferencesFactory(): PreferencesFactory = PreferencesFactoryImpl()

inline fun GeneralSettingsPreferences.update(block: MutableGeneralSettings.() -> Unit) =
  update(this.mutable().apply(block))

inline fun ProfileDataPreferences.update(block: MutableProfileData.() -> Unit) =
  update(this.mutable().apply(block))
