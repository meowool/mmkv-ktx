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

enableFeaturePreview("STABLE_CONFIGURATION_CACHE")
enableFeaturePreview("TYPESAFE_PROJECT_ACCESSORS")

dependencyResolutionManagement {
  repositoriesMode.set(RepositoriesMode.FAIL_ON_PROJECT_REPOS)
  repositories {
    google { filterAndroidDependencies(include = true) }
    mavenCentral { filterAndroidDependencies(include = false) }
  }
}

pluginManagement.repositories {
  gradlePluginPortal { filterAndroidDependencies(include = false) }
  mavenCentral { filterAndroidDependencies(include = false) }
  google { filterAndroidDependencies(include = true) }
}

// Include all possible subprojects (excluding root project and hidden directories)
rootDir.walkTopDown()
  .onEnter { it.name.first() != '.' }
  .filter { it.isDirectory && it.resolve("build.gradle.kts").exists() }
  .map { it.relativeTo(rootDir).path.replace(File.separatorChar, ':') }
  .filter { it.isNotEmpty() }
  .forEach { include(":$it") }

rootProject.name = "mmkv-ktx"

/**
 * Filtering Android dependencies across different repositories can reduce network
 * requests and speed up Gradle build times. This is because we are aware that these
 * dependencies are only available in Google Maven repository.
 */
fun ArtifactRepository.filterAndroidDependencies(include: Boolean) = content {
  arrayOf(
    "androidx\\..*",
    "com\\.android.*",
    "com\\.google\\.testing\\..*",
  ).forEach { group ->
    if (include) includeGroupByRegex(group) else excludeGroupByRegex(group)
  }
}
