enableFeaturePreview("TYPESAFE_PROJECT_ACCESSORS")

rootProject.name = "viabackwards-parent"

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
        id("org.jetbrains.gradle.plugin.idea-ext") version "1.1.9"
        id("com.gradleup.shadow") version "8.3.5"
    }
}

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
