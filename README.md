# MMKV-KTX

MMKV-KTX is a library that can help you observe the latest values of MMKV with Kotlin Flow.

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
