# MMKV-KTX

MMKV-KTX is an extension of [**MMKV**](https://github.com/Tencent/MMKV) designed to offer a seamless key-value storage solution for Kotlin, integrating tightly with the language's syntax and features.

## Features

- **Kotlin-Friendly**: Designed specifically for Kotlin, offering idiomatic syntax and integration.
- **Class Mapping**: Automatically maps Kotlin data classes to key-value storage for easy use.
- **Type Safety**: Supports custom type converters to securely store a wide range of data types.
- **Compile-Time Processing**: Employs [Kotlin Symbol Processing (KSP)](https://github.com/google/ksp) for compile-time code generation, ensuring type safety and reducing runtime overhead.

## Table of Contents
- [Features](#features)
- [Getting Started](#getting-started)
  - [Apply KSP Plugin](#apply-ksp-plugin)
  - [Configure KSP Argument](#configure-ksp-argument)
  - [Import Dependencies](#import-dependencies)
- [Basic Usage](#basic-usage)
  - [Initialization](#initialization)
  - [Declaring Data and Converters](#declaring-data-and-converters)
  - [Reading and Writing Preferences](#reading-and-writing-preferences)
  - [Reactive Support (Kotlin Flow)](#reactive-support-kotlin-flow)
- [Advanced](#advanced)
  - [Integrated with ViewModel and Hilt](#integrated-with-viewmodel-and-hilt)
  - [Integrated with Jetpack Compose](#integrated-with-jetpack-compose)
- [Contributing](#contributing)

## Getting Started

### Apply KSP Plugin

MMKV-KTX relies on compile-time code generation, so please make sure that KSP is [enabled in your module's `build.gradle.kts`](https://kotlinlang.org/docs/ksp-quickstart.html#use-your-own-processor-in-a-project):

```kotlin
plugins {
  id("com.google.devtools.ksp") version "<ksp_version>" // Replace it with your desired version
}
```

> [!IMPORTANT]
>
> Select a [KSP version](https://github.com/google/ksp/releases) compatible with your Kotlin version.

### Configure KSP Argument

Specify the package name for the generated code in your `build.gradle.kts`:

```kotlin
ksp.arg("mmkv.ktx.packageName", "<your_package>") // Replace it with your desired package, e.g. 'com.meowool.myapp.codegen'
```

### Import Dependencies

Depending on your project setup, follow the appropriate steps to include MMKV-KTX in your project.

<details>
  <summary>without <b>Version Catalog</b></summary>

>**build.gradle.kts**
```kotlin
dependencies {
  val mmkvKtxVersion = "0.1.4"
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
mmkv-ktx = "0.1.4"

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

MMKV-KTX is published on **Maven Central**, so if you **haven't** defined the repository yet, please do it:

```kotlin
repositories {
  mavenCentral()
}
```

## Basic Usage

MMKV-KTX greatly simplifies the usage of key-value storage in Kotlin. As a result, you can almost immediately understand how to use it just by looking at the following example.

### Initialization

Before starting, please make sure you have initialized the [MMKV](https://github.com/Tencent/MMKV#quick-tutorial) instance. If not, initialize it anywhere that can access the `Context`, such as in the `Application`:

```kotlin
class YourApplication : Application() {
  override fun onCreate(savedInstanceState: Bundle?) {
    super.onCreate(savedInstanceState)
    MMKV.initialize(this)
  }
}
```

### Declaring Data and Converters

Use annotations to define your preferences:

```kotlin
import java.util.UUID
import com.meowool.mmkv.ktx.Preferences
import com.meowool.mmkv.ktx.PersistDefaultValue

// @androidx.compose.runtime.Immutable (If you use Jetpack Compose, you can mark it as immutable)
@Preferences
data class UserSettings(
  val themeAppearance: ThemeAppearance = ThemeAppearance.Auto,
  val customToken: String? = null,
  val notificationsEnabled: Boolean = true,
)

// @androidx.compose.runtime.Immutable
@Preferences
data class GlobalData(
  @PersistDefaultValue // If needed
  val id: UUID = UUID.randomUUID(),
  val isLoggedIn: Boolean = true,
)

enum class ThemeAppearance { Light, Dark, Auto }
```

In this example, we have data of primitive types, enum types, and `UUID`. While the MMKV-KTX compiler supports most types by default, allowing for an out-of-the-box experience, using unsupported types (like `UUID` in our example) requires custom converters.

> For more information on supported types, please refer to [Annotation Document](https://github.com/meowool/mmkv-ktx/blob/main/runtime/src/main/kotlin/Preferences.kt#L29-L43)

Define your type converters:

```kotlin
import com.meowool.mmkv.ktx.TypeConverters

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
```

This setup allows you to freely convert custom types, ensuring that the MMKV-KTX compiler can handle a wide range of data types without built-in support.

### Reading and Writing Preferences

All preference instances are encapsulated within the `PreferencesFactory` object, making it the central point for operations. In the example provided above, we can access the `PreferencesFactory.userSettings` and `PreferencesFactory.globalData` properties by instantiating a `PreferencesFactory` object.

```kotlin
// Declare top-level static instance or inject a singleton with the DI pattern (e.g. Hilt)
val preferences: PreferencesFactory = PreferencesFactory()

fun anywhere() {
  println(preferences.globalData.get().id)
  println(preferences.userSettings.get().notificationsEnabled)
}
```
> The **`userSettings`** and **`globalData`** properties are automatically named based on the names of the preference data classes. You can also customize their names using `@Preferences(name = "customPropertyName")`.


Updating preferences is equally straightforward:

```kotlin
fun resetUserSettings() = preferences.userSettings.update {
  it.themeAppearance = ThemeAppearance.Auto
  it.customToken = null
  it.notificationsEnabled = true
}
```

### Reactive Support (Kotlin Flow)

Sometimes you may also want to be able to get the latest value in real time. MMKV-KTX provides `kotlinx.flow` conversion to make things incredibly easy.

To instantly react whenever the data changes, you can use the **`asStateFlow`** function to convert preferences into a hot flow (**StateFlow**) that you are familiar with (assuming you often use **Kotlin Flow**).

```kotlin
val userSettings: StateFlow<UserSettings> = preferences.userSettings.asStateFlow()

suspend fun log() = userSettings.collect {
  println("UserSettings updated: $it")
}
```

Additionally, you can use `mapStateFlow` to map changes to another value whenever data changes occur:

```kotlin
val notifications: StateFlow<NotificationsController?> = preferences.userSettings.mapStateFlow {
  if (it.notificationsEnabled) Factory.notificationsController else null
}
```

**For more detailed usage instructions and examples, please refer to the documentation for each annotation/function.**

## Advanced

In case you are interested in other usage methods, here are some common uses listed here.

### Integrated with ViewModel and Hilt

```kotlin
import dagger.Module
import dagger.Provides
import dagger.hilt.InstallIn
import dagger.hilt.components.SingletonComponent
import javax.inject.Singleton
import <your_package>.PreferencesFactory
import <your_package>.GlobalDataPreferences
import <your_package>.UserSettingsPreferences

@Module
@InstallIn(SingletonComponent::class)
class PreferencesModule {
  @Provides
  @Singleton
  fun providesPreferencesFactory(): PreferencesFactory = PreferencesFactory()

  @Provides
  fun providesGlobalDataPreferences(preferences: PreferencesFactory): GlobalDataPreferences = preferences.globalData

  @Provides
  fun providesUserSettingsPreferences(preferences: PreferencesFactory): UserSettingsPreferences = preferences.userSettings
}
```

```kotlin
class SettingsViewModel(
  private val globalData: GlobalDataPreferences,
  private val userSettings: UserSettingsPreferences,
) : ViewModel() {
  val followingSystemTheme = userSettings.mapStateFlow {
    it.themeAppearance == ThemeAppearance.Auto
  }
  
  fun saveToken(value: String) {
    checkToken(value) { ... }
    userSettings.update {
      it.customToken = TokenFactory.newTokenString(
        appId = globalData.get().id,
        token = value,
      )
    }
  }
}
```

### Integrated with Jetpack Compose

In the world of **Jetpack Compose**, there's nothing particularly special about using it, just for your reference:

```kotlin
@Composable
fun AppTheme(content: @Composable () -> Unit) {
  val systemInDark = isSystemInDarkTheme()
  val preferences: PreferencesFactory = remember { PreferencesFactory() }
  val usingDarkTheme by preferences.userSettings.mapStateFlow { setting ->
    when (setting.themeAppearance) {
      ThemeAppearance.Light -> false
      ThemeAppearance.Dark -> true
      ThemeAppearance.Auto -> systemInDark
    }
  }.collectAsStateWithLifecycle(initialValue = systemInDark)

  MaterialTheme(
    colors = if (usingDarkTheme) DarkColorPalette else LightColorPalette,
    content = content,
  )
}
```

## Contributing

Contributions to MMKV-KTX are welcome! Please feel free to submit pull requests or open issues to improve the library or documentation.

## License

MMKV-KTX is released under the **Apache License 2.0**. See the **[LICENSE](./LICENSE)** file for more details.

