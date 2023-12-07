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

import com.android.build.api.dsl.ApplicationDefaultConfig
import com.android.build.api.dsl.CommonExtension
import com.android.build.gradle.BasePlugin
import com.vanniktech.maven.publish.MavenPublishBaseExtension
import com.vanniktech.maven.publish.MavenPublishPlugin
import com.vanniktech.maven.publish.SonatypeHost

val isCiEnv = System.getenv("CI") != null
val isRelease = System.getenv("RELEASE") != null

plugins {
  alias(libs.plugins.detekt)
  alias(libs.plugins.android.library) apply false
  alias(libs.plugins.android.application) apply false
  alias(libs.plugins.kotlin.android) apply false
  alias(libs.plugins.kotlin.jvm) apply false
  alias(libs.plugins.kotlin.parcelize) apply false
  alias(libs.plugins.maven.publish) apply false
}

detekt {
  buildUponDefaultConfig = true
  parallel = isCiEnv
  config.setFrom(layout.projectDirectory.file("detekt.yml"))
}

allprojects {
  group = "com.meowool"
  version = "0.1.0" + if (isCiEnv && !isRelease) "-SNAPSHOT" else ""
  project.configureAndroid()
  project.configurePublish()
}

fun Project.configureAndroid() = plugins.withType<BasePlugin> {
  (extensions["android"] as CommonExtension<*, *, *, *, *>).apply {
    compileSdk = 34
    testOptions.unitTests.isIncludeAndroidResources = true
    defaultConfig {
      minSdk = 16
      testInstrumentationRunner = "androidx.test.runner.AndroidJUnitRunner"
      if (this is ApplicationDefaultConfig) targetSdk = 34
    }
    compileOptions {
      sourceCompatibility = JavaVersion.VERSION_17
      targetCompatibility = JavaVersion.VERSION_17
    }
  }
}

fun Project.configurePublish() = plugins.withType<MavenPublishPlugin> {
  extensions.configure<MavenPublishBaseExtension> {
    publishToMavenCentral(SonatypeHost.S01)
    signAllPublications()
    coordinates(
      groupId = project.group.toString(),
      artifactId = "mmkv" + if (name != "runtime") "-$name" else "",
      version = project.version.toString(),
    )
    pom {
      name.set("MMKV-KTX")
      description.set("A strong Kotlin-flavored MMKV extension library.")
      inceptionYear.set("2023")
      url.set("https://github.com/meowool/mmkv-ktx")
      licenses {
        license {
          name.set("The Apache License, Version 2.0")
          url.set("http://www.apache.org/licenses/LICENSE-2.0.txt")
          distribution.set("http://www.apache.org/licenses/LICENSE-2.0.txt")
        }
        // Tencent MMKV is licensed under the BSD 3-Clause License
        license {
          name.set("The BSD 3-Clause License")
          url.set("https://opensource.org/licenses/BSD-3-Clause")
          distribution.set("https://opensource.org/licenses/BSD-3-Clause")
        }
      }
      developers {
        developer {
          id.set("chachako")
          name.set("Cha")
          url.set("https://github.com/chachako/")
        }
      }
      scm {
        url.set("https://github.com/meowool/mmkv-ktx")
        connection.set("scm:git:git://github.com/meowool/mmkv-ktx.git")
        developerConnection.set("scm:git:ssh://git@github.com/meowool/mmkv-ktx.git")
      }
    }
  }
}
