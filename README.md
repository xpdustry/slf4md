# slf4md

[![Downloads](https://img.shields.io/github/downloads/xpdustry/slf4md/total?color=008080&label=Downloads)](https://github.com/xpdustry/slf4md/releases)
[![Mindustry 8.0](https://img.shields.io/badge/Mindustry-8.0-008080)](https://github.com/Anuken/Mindustry/releases)
[![Discord](https://img.shields.io/discord/519293558599974912?color=008080&label=Discord)](https://discord.xpdustry.com)

## Description

A mod providing a simple SLF4J implementation for Mindustry mods/plugins.
All it does it redirecting SLF4J logger (`org.slf4j.Logger`) to Arc logger (`arc.util.Log`).

## Installation

The mod requires:

- Java 8 or above.
- Mindustry v154 or above.

## Usage

For server owners, this mod comes with 3 config accessible with the `config` command:
- `loggerTrace`: Enable trace logging when debug is enabled. Disabled by default.
- `loggerDisplayMod`: Prepends the owning plugin name of a logger. Enabled by default.
- `loggerDisplayClass`: Prepends the owning class of a logger. Disabled by default.

## For developers

You only need to "compileOnly" `slf4j-api` in your `build.gradle`:

```kt
repositories {
    mavenCentral()
}

dependencies {
    compileOnly("org.slf4j:slf4j-api:$VERSION")
}
```

and add `slf4md` in the dependencies of your `plugin.json`:

```json
{
  "dependencies": ["slf4md"]
}
```

For testing, I recommend using the [toxopid](https://github.com/xpdustry/toxopid) gradle plugin, you will be able to automatically download this mod alongside yours:

<details open>
<summary>Gradle</summary>

```groovy
import com.xpdustry.toxopid.task.GithubAssetDownload
import com.xpdustry.toxopid.task.MindustryExec

plugins {
    id "com.xpdustry.toxopid" version "4.x.x"
}

def downloadSlf4md = tasks.register("downloadSlf4md", GithubAssetDownload) {
    owner = "xpdustry"
    repo = "slf4md"
    asset = "slf4md.jar"
    version = "v1.x.x"
}

tasks.withType(MindustryExec).configureEach {
    mods.from(downloadSlf4md)
}
```
</details>


<details>
<summary>Kotlin</summary>

```kt
import com.xpdustry.toxopid.task.GithubAssetDownload
import com.xpdustry.toxopid.task.MindustryExec

plugins {
    id("com.xpdustry.toxopid") version "4.x.x"
}

val downloadSlf4md by tasks.registering(GithubAssetDownload::class) {
    owner = "xpdustry"
    repo = "slf4md"
    asset = "slf4md.jar"
    version = "v1.x.x"
}

tasks.withType<MindustryExec> {
    mods.from(downloadSlf4md)
}
```
</details>

## Building

The mod requires Java 25 for compilation.

- `./gradlew :mergeJar` to compile the plugin into a usable jar (will be located at `builds/libs/slf4md.jar`).
- `./gradlew :runMindustryServer` to run the plugin in a local Mindustry server.
- `./gradlew :runMindustryDesktop` to start a local Mindustry client that will let you test the plugin.
