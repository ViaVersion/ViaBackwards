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

// Task to quickly test/debug code changes using https://github.com/ViaVersion/ViaProxy
// For further instructions see the ViaProxy repository README
val prepareViaProxyFiles by tasks.registering(Copy::class) {
    dependsOn(tasks.named("jar"))

    val projectName = project.name
    from(layout.buildDirectory.file("libs/${projectName}.jar"))
    into(layout.projectDirectory.dir("run/jars"))
    rename { "${projectName}.jar" }
}

val cleanupViaProxyFiles by tasks.registering(Delete::class) {
    delete(
        layout.projectDirectory.file("run/jars/${project.name}.jar"),
        layout.projectDirectory.dir("run/logs")
    )
}

val viaProxyConfiguration: Configuration by configurations.creating {
    dependencies.add(rootProject.libs.viaProxy.get().copy().setTransitive(false))
}

tasks.register<JavaExec>("runViaProxy") {
    dependsOn(prepareViaProxyFiles)
    finalizedBy(cleanupViaProxyFiles)

    mainClass.set("net.raphimc.viaproxy.ViaProxy")
    classpath = viaProxyConfiguration
    workingDir = layout.projectDirectory.dir("run").asFile
    jvmArgs = listOf("-DskipUpdateCheck")

    if (System.getProperty("viaproxy.gui.autoStart") != null) {
        jvmArgs("-Dviaproxy.gui.autoStart")
    }
}
