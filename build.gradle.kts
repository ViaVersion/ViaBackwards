import net.kyori.indra.IndraPlugin
import net.kyori.indra.IndraPublishingPlugin
import net.kyori.indra.sonatypeSnapshots

plugins {
    `java-library`
    `maven-publish`
    id("net.kyori.indra")
}

group = "com.viaversion"
version = "3.3.0-21w07a"
description = "Allow older clients to join newer server versions."

subprojects {
    apply<JavaLibraryPlugin>()
    apply<MavenPublishPlugin>()
    apply<IndraPlugin>()
    apply<IndraPublishingPlugin>()

    tasks {
        // Variable replacements
        processResources {
            filesMatching(listOf("plugin.yml", "mcmod.info", "fabric.mod.json", "bungee.yml")) {
                expand("version" to project.version,
                        "description" to project.description,
                        "url" to "https://github.com/ViaVersion/ViaBackwards")
            }
        }
        withType<JavaCompile> {
            options.compilerArgs.addAll(listOf("-nowarn", "-Xlint:-unchecked", "-Xlint:-deprecation"))
        }
    }

    val platforms = listOf(
        "bukkit",
        "bungee",
        "sponge",
        "velocity",
        "fabric"
    ).map { "viabackwards-$it" }
    if (platforms.contains(project.name)) {
        configureShadowJar()
    }

    repositories {
        sonatypeSnapshots()
        maven("https://hub.spigotmc.org/nexus/content/repositories/snapshots")
        maven("https://nexus.velocitypowered.com/repository/velocity-artifacts-snapshots/")
        maven("https://repo.spongepowered.org/maven")
        maven("https://repo.viaversion.com")
        maven("https://repo.maven.apache.org/maven2/")
        mavenLocal()
    }

    indra {
        javaVersions {
            target.set(8)
            testWith(8, 11, 15)
        }
        github("ViaVersion", "ViaBackwards") {
            issues = true
        }
        mitLicense()
    }

    publishing.repositories.maven {
        name = "Via"
        url = uri("https://repo.viaversion.com/")
        credentials(PasswordCredentials::class)
        authentication {
            create<BasicAuthentication>("basic")
        }
    }
}

tasks {
    withType<Jar> {
        onlyIf { false }
    }
}
