import com.diffplug.gradle.spotless.SpotlessExtension
import com.diffplug.gradle.spotless.SpotlessPlugin
import com.diffplug.spotless.FormatterFunc
import com.github.jengelman.gradle.plugins.shadow.ShadowJavaPlugin
import com.github.jengelman.gradle.plugins.shadow.ShadowPlugin
import com.github.jengelman.gradle.plugins.shadow.tasks.ShadowJar
import com.xpdustry.toxopid.ToxopidExtension
import com.xpdustry.toxopid.extension.anukeXpdustry
import com.xpdustry.toxopid.plugin.ToxopidPlugin
import com.xpdustry.toxopid.spec.ModMetadata
import com.xpdustry.toxopid.spec.ModPlatform
import net.kyori.indra.IndraExtension
import net.kyori.indra.IndraPlugin
import net.ltgt.gradle.errorprone.CheckSeverity
import net.ltgt.gradle.errorprone.ErrorPronePlugin
import net.ltgt.gradle.errorprone.errorprone

plugins {
    alias(libs.plugins.spotless) apply false
    alias(libs.plugins.indra.common) apply false
    alias(libs.plugins.indra.git)
    // alias(libs.plugins.indra.publishing) apply false
    alias(libs.plugins.shadow) apply false
    alias(libs.plugins.toxopid) apply false
    alias(libs.plugins.errorprone.gradle) apply false
}

version = "1.0.2" + if (indraGit.headTag() == null) "-SNAPSHOT" else ""
group = "com.xpdustry"
description = "A SLF4J implementation for Mindustry servers."

val release by tasks.registering(Copy::class) {
    destinationDir = temporaryDir
}

allprojects {
    apply<SpotlessPlugin>()

    repositories {
        mavenCentral()
        anukeXpdustry()
    }

    with(extensions.getByType<SpotlessExtension>()) {
        plugins.withType<JavaPlugin> {
            java {
                palantirJavaFormat()
                importOrder("", "\\#")
                // I will kill someone
                custom(
                    "no-wildcard-imports",
                    object : FormatterFunc, java.io.Serializable {
                        override fun apply(input: String) = input.apply { if (contains("*;\n")) error("No wildcard imports allowed") }
                    },
                )
                licenseHeaderFile(rootProject.file("HEADER.txt"))
                bumpThisNumberIfACustomStepChanges(1)
            }
        }
        kotlinGradle {
            ktlint()
        }
    }
}

subprojects {
    apply<IndraPlugin>()
    // apply<IndraPublishingPlugin>()
    apply<ShadowPlugin>()
    apply<ToxopidPlugin>()
    apply<ErrorPronePlugin>()

    val metadata = ModMetadata.fromJson(rootProject.file("plugin.json"))
    val local = ModMetadata.fromJson(file("plugin.json"))
    metadata.displayName = local.displayName
    metadata.description = local.description
    metadata.mainClass = local.mainClass
    metadata.version = rootProject.version.toString()

    val toxopid = extensions.getByType<ToxopidExtension>()
    toxopid.compileVersion = "v${metadata.minGameVersion}"
    toxopid.platforms = setOf(ModPlatform.SERVER)

    dependencies {
        val compileOnly by configurations
        val compileOnlyApi by configurations
        val annotationProcessor by configurations
        val errorprone by configurations
        val api by configurations

        api(rootProject.libs.slf4j.api)
        api(rootProject.libs.slf4j.from.jul)

        compileOnly(toxopid.dependencies.mindustryCore)
        compileOnly(toxopid.dependencies.arcCore)
        compileOnlyApi(rootProject.libs.checker.qual)
        annotationProcessor(rootProject.libs.nullaway)
        errorprone(rootProject.libs.errorprone.core)
    }

    // val signing = extensions.getByType<SigningExtension>()
    // val signingKey: String? by project
    // val signingPassword: String? by project
    // signing.useInMemoryPgpKeys(signingKey, signingPassword)

    with(extensions.getByType<IndraExtension>()) {
        javaVersions {
            target(17)
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
        outputs.files(fileTree(temporaryDir))
        doLast {
            temporaryDir.resolve("plugin.json").writeText(ModMetadata.toJson(metadata))
        }
    }

    val shadowJar =
        tasks.named<ShadowJar>(ShadowJavaPlugin.SHADOW_JAR_TASK_NAME) {
            archiveFileName = "${project.name}.jar"
            archiveClassifier = "plugin"
            from(generateResources)
            from(rootProject.file("LICENSE.md")) { into("META-INF") }
        }

    tasks.withType<JavaCompile> {
        options.errorprone {
            disableWarningsInGeneratedCode = true
            disable("MissingSummary", "InlineMeSuggester")
            check("NullAway", if (name.contains("test", ignoreCase = true)) CheckSeverity.OFF else CheckSeverity.ERROR)
            option("NullAway:AnnotatedPackages", "com.xpdustry.slf4md")
        }
    }

    release.configure {
        from(shadowJar)
    }
}
