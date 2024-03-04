# MMKV-KTX

MMKV-KTX is an extension of [**MMKV**](https://github.com/Tencent/MMKV) that provides another way to use key-value storage in Kotlin. 

> This library is tightly integrated with Kotlin language features and also supports real-time data reception with [**StateFlow**](https://developer.android.com/kotlin/flow/stateflow-and-sharedflow#stateflow), enabling you to easily listen to the latest values from anywhere.

## Usage

```kotlin
@TypeConverters
class Converters {
  fun UUID.toBytes(): ByteArray = ByteBuffer.allocate(16).apply {
    putLong(mostSignificantBits)
    putLong(leastSignificantBits)
  }.array()
  
  fun ByteArray.toUUID(): UUID = with(ByteBuffer.wrap(this)) {
    UUID(long, long)
  }
}

########################

enum class ThemeAppearance {
  Light, Dark, Auto,
}

@Immutable // If used in Jetpack Compose
@Preferences
data class GeneralSettings(
  val themeAppearance: ThemeAppearance = ThemeAppearance.Auto,
  val customToken: String? = null,
  val checkUpdates: Boolean = true,
)

@Immutable // If used in Jetpack Compose
@Preferences
data class GlobalData(
  @PersistDefaultValue // If needed
  val lastUser: UUID = UUID.randomUUID(),
  val isLoggedIn: Boolean = true,
)

########################

// Declare top-level static instance or inject a singleton with the DI framework (Hilt)
val preferences: PreferencesFactory = PreferencesFactory()

@Composable
fun App() {
  val systemInDark = isSystemInDarkTheme()
  val usesDark = preferences.generalSettings.mapStateFlow {
    when (it.themeAppearance) {
      Light -> false
      Dark -> true
      Auto -> systemInDark
    }
  }.collectAsStateWithLifecycle()
  
  ...
}

########################

class SettingsViewModel(private val preferences: PreferencesFactory) : ViewModel() {
  val generalSettings = preferences.generalSettings.asStateFlow()
  
  fun saveToken(value: String) {
    checkToken(value) { ... }
    preferences.generalSettings.update {
      it.customToken = value
    }
  }
}
```

## Setup

### 1. Apply KSP plugin

MMKV-KTX relies on compile-time code generation, so please make sure that KSP is [enabled in your module](https://kotlinlang.org/docs/ksp-quickstart.html#use-your-own-processor-in-a-project):

```kotlin
plugins {
  id("com.google.devtools.ksp") version "<ksp_version>" // Replace it you desire
}
```

> [!IMPORTANT]
>
> Choose a [KSP version in the release](https://github.com/google/ksp/releases) that is compatible with your project's Kotlin version.

### 2. Import dependencies

<details>
  <summary>without <b>Version Catalog</b></summary>

```kotlin
dependencies {
  val mmkvKtxVersion = "0.1.3"
  implementation("com.meowool:mmkv:$mmkvKtxVersion")
  ksp("com.meowool:mmkv-compiler:$mmkvKtxVersion")
}
```
</details>


<details open>
  <summary>using <b>Version Catalog</b></summary>

>
> **libs.versions.toml**
```toml
[versions]
mmkv-ktx = "0.1.3"

[librarys]
mmkv-ktx = { module = "com.meowool:mmkv", version.ref = "mmkv-ktx" }
mmkv-ktx-compiler = { module = "com.meowool:mmkv-compiler", version.ref = "mmkv-ktx" }
```

> **build.gradle.kts**
```kotlin
dependencies {
  implementation(mmkv.ktx)
  ksp(mmkv.ktx.compiler)
}
```
</details>

Finally, set the ksp parameter in `build.gradle.kts`

`build.gradle.ktx (app)`

```
ksp.arg("mmkv.ktx.packageName", "$group.codegen")
```

MMKV-KTX is published on **Maven Central**, so if you haven't defined the repository yet, please do it:
```kotlin
repositories {
  mavenCentral()
}
```

### 3. Configure code generation

Define the output package name for the generated code:
> **build.gradle.kts**
```kotlin
ksp.arg("mmkv.ktx.packageName", "<your_package>")
```
