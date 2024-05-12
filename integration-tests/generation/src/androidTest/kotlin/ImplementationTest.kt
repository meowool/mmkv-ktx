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

import androidx.test.ext.junit.runners.AndroidJUnit4
import androidx.test.platform.app.InstrumentationRegistry
import assertk.assertThat
import assertk.assertions.matchesPredicate
import com.meowool.codegen.PreferencesFactory
import com.meowool.codegen.update
import com.meowool.mmkv.ktx.tests.model.PrimitiveData
import com.meowool.mmkv.ktx.tests.utils.Json
import com.tencent.mmkv.MMKV
import org.junit.Before
import org.junit.runner.RunWith
import kotlin.test.Test
import kotlin.test.assertEquals

@RunWith(AndroidJUnit4::class)
class ImplementationTest {
  @Before
  fun setUp() {
    MMKV.initialize(InstrumentationRegistry.getInstrumentation().targetContext)
  }

  @Test
  fun testPersistDefaultValue() {
    val aPreferences = preferencesFactory()
    val bPreferences = preferencesFactory()

    assertEquals(
      aPreferences.customData.get().date,
      bPreferences.customData.get().date,
    )

    assertEquals(
      aPreferences.primitiveData.get().enum,
      bPreferences.primitiveData.get().enum,
    )

    assertEquals(
      aPreferences.primitiveData.get().enumNullable,
      bPreferences.primitiveData.get().enumNullable,
    )

    assertEquals(
      aPreferences.primitiveData.get().parcelable,
      bPreferences.primitiveData.get().parcelable,
    )

    assertEquals(
      aPreferences.primitiveData.get().parcelableNullable,
      bPreferences.primitiveData.get().parcelableNullable,
    )

    assertEquals(
      aPreferences.primitiveData.get().bool,
      bPreferences.primitiveData.get().bool,
    )

    assertEquals(
      aPreferences.primitiveData.get().boolNullable,
      bPreferences.primitiveData.get().boolNullable,
    )
  }

  @Test
  fun testUpdate() {
    val preferences = preferencesFactory()

    val newData = PrimitiveData(
      enum = PrimitiveData.Enum.Second,
      string = "Hello, MMKV-KTX!",
      stringNullable = "",
    )

    preferences.primitiveData.update {
      it.enum = newData.enum
      it.string = newData.string
      it.stringNullable = newData.stringNullable
    }

    assertThat(preferences.primitiveData.get()).matchesPredicate {
      it.enum == newData.enum &&
        it.string == newData.string &&
        it.stringNullable == newData.stringNullable
    }

    assertEquals(
      preferences.primitiveData.get().enum,
      preferencesFactory().primitiveData.get().enum,
    )
  }

  private fun preferencesFactory(): PreferencesFactory =
    PreferencesFactory(ConstructedConverters(Json()))
}
