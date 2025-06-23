# slf4md

[![Build status](https://github.com/xpdustry/template-plugin/actions/workflows/build.yml/badge.svg?branch=master&event=push)](https://github.com/xpdustry/template-plugin/actions/workflows/build.yml)
[![Mindustry 7.0](https://img.shields.io/badge/Mindustry-7.0-ffd37f)](https://github.com/Anuken/Mindustry/releases)

## Description

A set of plugins providing various SLF4J implementations for Mindustry.

## Installation

The plugins require:

- Java 17 or above.

- Mindustry v146 or above.

## Usage

### For server owners

If one of your plugins requires `slf4md`, you don't have to do anything special.
Simply choose the implementation that suits your needs and install it on your server (in the `config/mods` directory):

- `slf4md-simple`: A logger that redirects to arc logger (`arc.util.Log`). 
  - No fancy stuff or invasive changes. 
  - Comes with 3 simple settings accessible with the command `config`:
    - `loggerTrace`: Enable trace logging when debug is enabled. Disabled by default.
    - `loggerDisplayPlugin`: Prepends the owning plugin name of a logger. Enabled by default.
    - `loggerDisplayClass`: Prepends the owning class of a logger. Disabled by default.

### For plugin developers

You only need to "compileOnly" `slf4j-api` in your `build.gradle`:

```gradle
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

## Building

- `./gradlew :slf4md-{module}:shadowJar` to compile the plugin into a usable jar (will be located
  at `builds/libs/(plugin-name).jar`).

- `./gradlew :slf4md-{module}:runMindustryServer` to run the plugin in a local Mindustry server.

- `./gradlew :slf4md-{module}:runMindustryDesktop` to start a local Mindustry client that will let you test the plugin.
