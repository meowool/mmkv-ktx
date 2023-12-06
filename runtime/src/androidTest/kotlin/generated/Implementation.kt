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

@file:Suppress("INVISIBLE_REFERENCE", "INVISIBLE_MEMBER", "NAME_SHADOWING")

package com.meowool.mmkv.ktx.test.generated

import com.meowool.mmkv.ktx.UNCHANGED
import com.meowool.mmkv.ktx.WRITE_ONLY
import com.meowool.mmkv.ktx.takeIfChanged
import com.meowool.mmkv.ktx.test.Converters.toBytes
import com.meowool.mmkv.ktx.test.Converters.toUUID
import com.meowool.mmkv.ktx.test.model.GeneralSettings
import com.meowool.mmkv.ktx.test.model.ProfileData
import com.meowool.mmkv.ktx.test.model.ThemeAppearance
import com.meowool.mmkv.ktx.test.model.Token
import com.tencent.mmkv.MMKV
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import java.util.UUID

@PublishedApi
internal class PreferencesFactoryImpl : PreferencesFactory {
  @Volatile private var _generalSettings: GeneralSettingsPreferences? = null
  @Volatile private var _profileData: ProfileDataPreferences? = null

  override val generalSettings: GeneralSettingsPreferences
    get() = synchronized(this) {
      _generalSettings ?: GeneralSettingsPreferencesImpl().also { _generalSettings = it }
    }

  override val profileData: ProfileDataPreferences
    get() = synchronized(this) {
      _profileData ?: ProfileDataPreferencesImpl().also { _profileData = it }
    }
}

internal class GeneralSettingsPreferencesImpl : GeneralSettingsPreferences {
  private val mmkv = MMKV.mmkvWithID("GeneralSettings")
  private val default = GeneralSettings()

  @Volatile private var _instance: GeneralSettings? = null

  @Volatile private var _state: MutableStateFlow<GeneralSettings>? = null

  override fun get(): GeneralSettings {
    val instance = _instance
    if (instance != null) return instance

    return synchronized(this) {
      val instance = _instance
      if (instance != null) return instance

      GeneralSettings(
        themeAppearance = run {
          val entries = ThemeAppearance.entries
          val ordinal = mmkv.decodeInt("themeAppearance", -1)
          if (ordinal == -1) return@run default.themeAppearance
          entries[ordinal]
        },
        customToken = mmkv.decodeParcelable("customToken", Token::class.java, default.customToken),
        checkUpdates = mmkv.decodeBool("checkUpdates", default.checkUpdates),
        checkTimes = mmkv.decodeStringSet("checkTimes") ?: default.checkTimes,
      ).also { _instance = it }
    }
  }

  override fun mutable(): MutableGeneralSettings = MutableGeneralSettingsImpl()

  override fun update(mutable: MutableGeneralSettings) {
    _state?.value = mutable.toImmutable()
  }

  override fun asStateFlow(): StateFlow<GeneralSettings> {
    val state = _state
    if (state != null) return state

    return synchronized(this) {
      val state = _state
      if (state != null) return state

      MutableStateFlow(get()).also { _state = it }
    }
  }

  inner class MutableGeneralSettingsImpl : MutableGeneralSettings {
    private var newThemeAppearance: Any? = UNCHANGED
    private var newCustomToken: Any? = UNCHANGED
    private var newCheckUpdates: Any? = UNCHANGED

    override var themeAppearance: ThemeAppearance
      get() = throw UnsupportedOperationException(WRITE_ONLY)
      set(value) {
        mmkv.encode("themeAppearance", value.ordinal)
        newThemeAppearance = value
      }

    override var customToken: Token?
      get() = throw UnsupportedOperationException(WRITE_ONLY)
      set(value) {
        mmkv.encode("customToken", value)
        newCustomToken = value
      }

    override var checkUpdates: Boolean
      get() = throw UnsupportedOperationException(WRITE_ONLY)
      set(value) {
        mmkv.encode("checkUpdates", value)
        newCheckUpdates = value
      }
    override var checkTimes: Set<String>
      get() = TODO("Not yet implemented")
      set(value) {}

    override fun toImmutable(): GeneralSettings = get().also { old ->
      GeneralSettings(
        themeAppearance = newThemeAppearance.takeIfChanged() ?: old.themeAppearance,
        customToken = newCustomToken.takeIfChanged() ?: old.customToken,
        checkUpdates = newCheckUpdates.takeIfChanged() ?: old.checkUpdates,
      )
    }
  }
}

internal class ProfileDataPreferencesImpl : ProfileDataPreferences {
  private val mmkv = MMKV.mmkvWithID("ProfileData")
  private val default = ProfileData()

  @Volatile private var _instance: ProfileData? = null

  @Volatile private var _state: MutableStateFlow<ProfileData>? = null

  override fun get(): ProfileData {
    val instance = _instance
    if (instance != null) return instance

    return synchronized(this) {
      val instance = _instance
      if (instance != null) return instance

      ProfileData(
        id = mmkv.decodeBytes("id")?.toUUID() ?: default.id,
        isLoggedIn = mmkv.decodeBool("isLoggedIn", default.isLoggedIn),
      ).also { _instance = it }
    }
  }

  override fun mutable(): MutableProfileData = MutableProfileDataImpl()

  override fun update(mutable: MutableProfileData) {
    _state?.value = mutable.toImmutable()
  }

  override fun asStateFlow(): StateFlow<ProfileData> {
    val state = _state
    if (state != null) return state

    return synchronized(this) {
      val state = _state
      if (state != null) return state

      MutableStateFlow(get()).also { _state = it }
    }
  }

  inner class MutableProfileDataImpl : MutableProfileData {
    private var newId: Any? = UNCHANGED
    private var newIsLoggedIn: Any? = UNCHANGED

    override var id: UUID
      get() = throw UnsupportedOperationException(WRITE_ONLY)
      set(value) {
        mmkv.encode("followedUsers", value.toBytes())
        newId = value
      }

    override var isLoggedIn: Boolean
      get() = throw UnsupportedOperationException(WRITE_ONLY)
      set(value) {
        mmkv.encode("isLoggedIn", value)
        newIsLoggedIn = value
      }

    override fun toImmutable(): ProfileData = get().also { old ->
      ProfileData(
        id = newId.takeIfChanged() ?: old.id,
        isLoggedIn = newIsLoggedIn.takeIfChanged() ?: old.isLoggedIn,
      )
    }
  }
}
