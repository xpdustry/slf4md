import com.xpdustry.toxopid.extension.anukeXpdustry
import com.xpdustry.toxopid.spec.ModMetadata
import com.xpdustry.toxopid.spec.ModPlatform
import com.xpdustry.toxopid.task.MindustryExec
import net.ltgt.gradle.errorprone.CheckSeverity
import net.ltgt.gradle.errorprone.errorprone

plugins {
    id("com.diffplug.spotless") version "8.1.0"
    id("net.kyori.indra") version "4.0.0"
    id("com.gradleup.shadow") version "9.3.1"
    id("com.xpdustry.toxopid") version "4.1.2"
    id("net.ltgt.errorprone") version "4.4.0"
}

val metadata = ModMetadata.fromJson(rootProject.file("mod.json"))
metadata.version += if (findProperty("release").toString().toBoolean()) "-SNAPSHOT" else ""
version = metadata.version
group = "com.xpdustry"
description = metadata.description

repositories {
    mavenCentral()
    anukeXpdustry()
}

spotless {
    java {
        palantirJavaFormat()
        importOrder("", "\\#")
        licenseHeaderFile(rootProject.file("HEADER.txt"))
    }
    kotlinGradle {
        ktlint()
    }
}

toxopid {
    compileVersion = "v" + metadata.minGameVersion
    platforms = setOf(ModPlatform.SERVER, ModPlatform.DESKTOP, ModPlatform.ANDROID)
}

dependencies {
    compileOnly(toxopid.dependencies.mindustryCore)
    compileOnly(toxopid.dependencies.arcCore)
    compileOnlyApi("org.jspecify:jspecify:1.0.0")
    api("org.slf4j:slf4j-api:2.0.17")
    api("org.slf4j:jul-to-slf4j:2.0.17")
    annotationProcessor("com.uber.nullaway:nullaway:0.12.15")
    errorprone("com.google.errorprone:error_prone_core:2.46.0")
}

indra {
    javaVersions {
        target(8)
        minimumToolchain(25)
    }

    publishSnapshotsTo("xpdustry", "https://maven.xpdustry.com/snapshots")
    publishReleasesTo("xpdustry", "https://maven.xpdustry.com/releases")

    mitLicense()

    if (metadata.repository.isNotBlank()) {
        val repo = metadata.repository.split("/")
        github(repo[0], repo[1]) {
            ci(true)
            issues(true)
            scm(true)
        }
    }

    configurePublications {
        pom {
            organization {
                name = "xpdustry"
                url = "https://www.xpdustry.com"
            }

            developers {
                developer {
                    id = "phinner"
                    timezone = "Europe/Brussels"
                }
            }
        }
    }
}

val generateResources by tasks.registering {
    inputs.property("metadata", metadata)
    outputs.files(fileTree(temporaryDir))
    doLast {
        temporaryDir.resolve("mod.json").writeText(ModMetadata.toJson(metadata))
    }
}

tasks.shadowJar {
    from(generateResources)
    from(rootProject.file("LICENSE.md")) { into("META-INF") }
}

tasks.mergeJar {
    archiveFileName = "${project.name}.jar"
    archiveClassifier = "mod"
}

tasks.build {
    dependsOn(tasks.mergeJar)
}

tasks.withType<JavaCompile> {
    options.errorprone {
        disableWarningsInGeneratedCode = true
        disable("MissingSummary", "InlineMeSuggester")
        check("NullAway", if (name.contains("test", ignoreCase = true)) CheckSeverity.OFF else CheckSeverity.ERROR)
        option("NullAway:AnnotatedPackages", "com.xpdustry.slf4md")
    }
}

tasks.withType<MindustryExec> {
    jvmArguments.add("--enable-native-access=ALL-UNNAMED")
}
