import com.xpdustry.toxopid.extension.anukeXpdustry
import com.xpdustry.toxopid.spec.ModMetadata
import com.xpdustry.toxopid.spec.ModPlatform
import com.xpdustry.toxopid.task.MindustryExec
import net.ltgt.gradle.errorprone.CheckSeverity
import net.ltgt.gradle.errorprone.errorprone

plugins {
    alias(libs.plugins.spotless)
    alias(libs.plugins.indra.common)
    alias(libs.plugins.indra.git)
    alias(libs.plugins.shadow)
    alias(libs.plugins.toxopid)
    alias(libs.plugins.errorprone.gradle)
}

val metadata = ModMetadata.fromJson(rootProject.file("mod.json"))
version = metadata.version + if (indraGit.headTag() == null) "-SNAPSHOT" else ""
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
    compileVersion = rootProject.libs.versions.mindustry
    platforms = setOf(ModPlatform.SERVER, ModPlatform.DESKTOP, ModPlatform.ANDROID)
}

dependencies {
    compileOnly(toxopid.dependencies.mindustryCore)
    compileOnly(toxopid.dependencies.arcCore)
    compileOnlyApi(libs.jspecify)
    api(libs.slf4j.api)
    api(libs.slf4j.from.jul)
    annotationProcessor(libs.nullaway)
    errorprone(libs.errorprone.core)
}

indra {
    javaVersions {
        target(8)
        minimumToolchain(17)
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
