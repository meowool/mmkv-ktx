# MMKV-KTX

MMKV-KTX is an extension of **[MMKV](https://github.com/Tencent/MMKV)** that provides another way to use key-value storage in Kotlin. It is highly integrated with Kotlin language features, and also supports receiving data in real-time with [StateFlow](https://developer.android.com/kotlin/flow/stateflow-and-sharedflow#stateflow), This makes it easier for you to listen to the latest values in Jetpack Compose.

## Usage:

Add the codes below to your root `build.gradle.kts` file (not your module-level build.gradle.kts file):

``` kotlin
allprojects {
    repositories {
        mavenCentral()
    }
}
```

Next, add the dependency below to your module's `build.gradle.kts` file And `libs.versions.toml` (if you are using version catalog):

`libs.toml`

```
[versions]
mmkv-ktx = "0.1.0"

[librarys]
mmkv-ktx = { module = "com.meowool:mmkv", version.ref = "mmkv-ktx" }
mmkv-ktx-compiler = { module = "com.meowool:mmkv-compiler", version.ref = "mmkv-ktx" }
```

`build.gradle.kts`

```
dependencies {
    implementation(mmkv.ktx)
    ksp(mmkv.ktx.compiler)
}
```
