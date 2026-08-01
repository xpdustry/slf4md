import com.xpdustry.toxopid.extension.anukeXpdustry
import com.xpdustry.toxopid.spec.ModMetadata
import com.xpdustry.toxopid.spec.ModPlatform
import com.xpdustry.toxopid.task.MindustryExec
import net.ltgt.gradle.errorprone.CheckSeverity
import net.ltgt.gradle.errorprone.errorprone

plugins {
    id("com.diffplug.spotless") version "8.5.1"
    id("net.kyori.indra") version "4.0.0"
    id("com.gradleup.shadow") version "9.4.1"
    id("com.xpdustry.toxopid") version "4.2.0"
    id("net.ltgt.errorprone") version "5.1.0"
}

val metadata = ModMetadata.fromJson(rootProject.file("mod.json"))
metadata.version += if (findProperty("is_release").toString().toBoolean()) "" else "-SNAPSHOT"
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
        formatAnnotations()
        importOrder("", "\\#")
        forbidModuleImports()
        forbidWildcardImports()
        licenseHeader("// SPDX-License-Identifier: MIT")
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
    compileOnlyApi("org.jspecify:jspecify:1.0.1")
    api("org.slf4j:slf4j-api:2.0.18")
    api("org.slf4j:jul-to-slf4j:2.0.18")
    annotationProcessor("com.uber.nullaway:nullaway:0.13.4")
    testAnnotationProcessor("com.uber.nullaway:nullaway:0.13.4")
    errorprone("com.google.errorprone:error_prone_core:2.49.0")
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

val generateMetadataFile by tasks.registering {
    inputs.property("metadata", metadata)
    val output = temporaryDir.resolve("plugin.json")
    outputs.file(output)
    doLast { output.writeText(ModMetadata.toJson(metadata)) }
}

tasks.shadowJar {
    from(generateMetadataFile)
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
        disable("MissingSummary", "InlineMeSuggester")
        option("NullAway:OnlyNullMarked")
        check("NullAway", CheckSeverity.ERROR)
    }
}

tasks.withType<MindustryExec> {
    jvmArguments.add("--enable-native-access=ALL-UNNAMED")
}
