plugins {
    id("net.kyori.blossom")
    id("org.jetbrains.gradle.plugin.idea-ext")
}

sourceSets {
    main {
        blossom {
            javaSources {
                property("version", project.version.toString())
                property("impl_version", "git-ViaBackwards-${project.version}:${rootProject.latestCommitHash()}")
            }
        }
    }
}

dependencies {
    compileOnlyApi(libs.viaver)
    compileOnlyApi(libs.netty)
    compileOnlyApi(libs.guava)
    compileOnlyApi(libs.checkerQual)
}

java {
    withJavadocJar()
}
