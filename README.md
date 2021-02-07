# Tuya-lib

`Tuya-lib` is an **EXPERIMENTAL** JVM library to control smart devices with Tuya firmware.

_EXPERIMENTAL_ status means that this library is in incubating state - I'm lurking around, trying things and breaking
them up. **Library API and structure will be changed without additional notices.** However, published maven artifacts
will not be removed.

## Features

**WARNING:** Only devices with firmware version 3.3 are supported for now.

- Turn on a `Bulb`.
- Turn off a `Bulb`.
- Check if a `Bulb` is turned on.

## Installation

1. Library is distributed via [MavenCentral](https://search.maven.org/artifact/space.dector.tuyalib/library). Ensure
   that you have it enabled in `build.gradle.kts`:

```kotlin
repositories {
    // ...
    mavenCentral()
}
```

<details><summary><i>For snapshots</i></summary>

```kotlin
repositories {
    // ...
    maven("https://maven.pkg.jetbrains.space/dector/p/tuya-lib/mvn")
}
```

</details>

2. Add `tuya-lib` to your dependencies:

```kotlin
dependencies {
    // ...
    implementation("space.dector.tuyalib:library:0.1.0")
}
```

<details><summary><i>For snapshots</i></summary>

```kotlin
repositories {
    // ...
    implementation("space.dector.tuyalib:library:0.1.1-SNAPSHOT")
}
```

</details>

3. Sync your project with Gradle and you are done.

## Usage

### Controlling a bulb

_You can check [`sample/`](sample/) directory for minimalistic library usage example._

**WARNING:** Only bulb with firmware version 3.3 is supported for now.

**WARNING:** Your bulb need to be activated (instructions will be provided later).

```kotlin
val bulb = Bulb(
    // bulb IP in local network
    ip = "192.168.0.17",

    // device ID in Tuya system
    deviceId = "1ca7b0eb833a56fcf449",

    // local AES key for device
    localKey = "3e3de0d819e34753",
)

bulb.turnOn()
```

Use small delay between executing operations. Devices are slow and might skip your commands if you are sending them too
fast.

## Changelog

See [CHANGELOG.md](CHANGELOG.md) file.

## License

This project is licensed under the [Apache 2.0](https://choosealicense.com/licenses/apache-2.0/) License - see
the [LICENSE.md](LICENSE.md) file for details.

## Acknowledgments

This library might never exist without these awesome projects and people behind them that did all the work for reversing
Tuya devices protocol:

- [tuya-convert](https://github.com/ct-Open-Source/tuya-convert)
- [TuyaAPI](https://github.com/TuyaAPI/)
