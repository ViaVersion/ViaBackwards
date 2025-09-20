enableFeaturePreview("TYPESAFE_PROJECT_ACCESSORS")

dependencyResolutionManagement {
    repositories {
        maven("https://repo.viaversion.com")
        maven("https://repo.papermc.io/repository/maven-public/")
        mavenCentral()
    }
    repositoriesMode.set(RepositoriesMode.FAIL_ON_PROJECT_REPOS)
}

pluginManagement {
    plugins {
        id("net.kyori.blossom") version "2.1.0"
        id("org.jetbrains.gradle.plugin.idea-ext") version "1.2"
        id("com.gradleup.shadow") version "9.1.0"
    }
}

plugins {
    id("org.gradle.toolchains.foojay-resolver-convention") version "1.0.0"
}

rootProject.name = "viabackwards-parent"

includeBuild("build-logic")

setupViaSubproject("common")
setupViaSubproject("bukkit")
setupViaSubproject("velocity")
setupViaSubproject("sponge")
setupViaSubproject("fabric")

setupSubproject("viabackwards") {
    projectDir = file("universal")
}

fun setupViaSubproject(name: String) {
    setupSubproject("viabackwards-$name") {
        projectDir = file(name)
    }
}

inline fun setupSubproject(name: String, block: ProjectDescriptor.() -> Unit) {
    include(name)
    project(":$name").apply(block)
}
