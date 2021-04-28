import com.github.jengelman.gradle.plugins.shadow.ShadowPlugin

plugins {
    `java-library`
    `maven-publish`
    id("net.kyori.blossom") version "1.2.0" apply false
}

allprojects {
    group = "com.viaversion"
    version = "4.0.0-21w17a"
    description = "Allow older clients to join newer server versions."
}

subprojects {
    apply<JavaLibraryPlugin>()
    apply<MavenPublishPlugin>()

    tasks {
        // Variable replacements
        processResources {
            filesMatching(listOf("plugin.yml", "mcmod.info", "fabric.mod.json", "bungee.yml")) {
                expand("version" to project.version,
                        "description" to project.description,
                        "url" to "https://github.com/ViaVersion/ViaBackwards")
            }
        }
        withType<Javadoc> {
            options.encoding = Charsets.UTF_8.name()
        }
        withType<JavaCompile> {
            options.encoding = Charsets.UTF_8.name()
            options.compilerArgs.addAll(listOf("-nowarn", "-Xlint:-unchecked", "-Xlint:-deprecation"))
        }
    }

    repositories {
        maven("https://repo.viaversion.com")
        maven("https://papermc.io/repo/repository/maven-public/")
        maven("https://oss.sonatype.org/content/repositories/snapshots/")
        maven("https://nexus.velocitypowered.com/repository/velocity-artifacts-snapshots/")
        maven("https://repo.spongepowered.org/maven")
        mavenCentral()
    }

    java {
        sourceCompatibility = JavaVersion.VERSION_1_8
        targetCompatibility = JavaVersion.VERSION_1_8
        withSourcesJar()
        withJavadocJar()
    }
}

// Configure shadow tasks before the publishing task
sequenceOf(
        projects.viabackwardsBukkit,
        projects.viabackwardsBungee,
        projects.viabackwardsFabric,
        projects.viabackwardsSponge,
        projects.viabackwardsVelocity
).map { it.dependencyProject }.forEach { project ->
    project.configureShadowJar()
}

projects.viabackwards.dependencyProject.apply<ShadowPlugin>()

subprojects {
    publishing {
        publications {
            create<MavenPublication>("mavenJava") {
                groupId = rootProject.group as String
                artifactId = project.name
                version = rootProject.version as String

                if (plugins.hasPlugin(ShadowPlugin::class.java)) {
                    artifact(tasks["shadowJar"])
                } else {
                    from(components["java"])
                }
            }
        }
        repositories.maven {
            name = "Via"
            url = uri("https://repo.viaversion.com/")
            credentials(PasswordCredentials::class)
            authentication {
                create<BasicAuthentication>("basic")
            }
        }
    }
}

tasks {
    withType<Jar> {
        onlyIf { false }
    }
}
