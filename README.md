# slf4md

[![Downloads](https://img.shields.io/github/downloads/xpdustry/slf4md/total?color=008080&label=Downloads)](https://github.com/xpdustry/slf4md/releases)
[![Mindustry 8.0](https://img.shields.io/badge/Mindustry-8.0-008080)](https://github.com/Anuken/Mindustry/releases)
[![Discord](https://img.shields.io/discord/519293558599974912?color=008080&label=Discord)](https://discord.xpdustry.com)

## Description

A mod providing a simple SLF4J implementation for Mindustry mods/plugins.
It redirects the SLF4J logger (`org.slf4j.Logger`) to the Arc logger (`arc.util.Log`).

## Installation

The mod requires:

- Java 8 or above.
- Mindustry v157 or above.

## Usage

For server owners, this mod exposes dedicated runtime commands for logger levels:

- `log-level-set <name> <trace|debug|info|warn|error|default>`: Set or clear a log level for a logger or mod.
- `log-level-list`: List every logger or mod with an explicit log level.

You can also configure the mod using the `config` command, with `config key value`:

- `log-show-class-name`: Prepend the logger class name to log statements. Default: `false`.
- `log-show-mod-name`: Prepend the mod name to log statements. Default: `true`.
- `trace`: Enable trace logging when debug logging is active. Default: `false`.

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
