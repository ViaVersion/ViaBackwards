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

    testImplementation(libs.viaver)
    testImplementation(libs.netty)
    testImplementation(libs.guava)
    testImplementation(libs.bundles.junit)
    testRuntimeOnly(libs.platformLauncher)
}

java {
    withJavadocJar()
}

// Task to quickly test/debug code changes using https://github.com/ViaVersion/ViaProxy
// For further instructions see the ViaProxy repository README
val prepareViaProxyFiles = tasks.register<Copy>("prepareViaProxyFiles") {
    description = "Prepares the ViaProxy run dir for the runViaProxy task"
    dependsOn(project.tasks.shadowJar)

    from(project.tasks.shadowJar.map { it.archiveFile.get().asFile })
    into(layout.projectDirectory.dir("run/jars"))

    val projectName = project.name
    rename { "${projectName}.jar" }
}

val cleanupViaProxyFiles = tasks.register<Delete>("cleanupViaProxyFiles") {
    description = "Deletes ViaProxy logs and the compiled project jar from its jars dir"
    delete(
        layout.projectDirectory.file("run/jars/${project.name}.jar"),
        layout.projectDirectory.dir("run/logs")
    )
}

val viaProxyConfiguration: Configuration = configurations.create("viaProxyConfiguration") {
    dependencies.add(rootProject.libs.viaProxy.get().copy().setTransitive(false))
}

tasks.register<JavaExec>("runViaProxy") {
    description = "Runs ViaProxy locally"
    dependsOn(prepareViaProxyFiles)
    finalizedBy(cleanupViaProxyFiles)

    mainClass.set("net.raphimc.viaproxy.ViaProxy")
    classpath = viaProxyConfiguration
    workingDir = layout.projectDirectory.dir("run").asFile
    jvmArgs = listOf("-DskipUpdateCheck", "-Dviaproxy.gui.autoStart")

    if (System.getProperty("viaproxy.disableExtraPlatforms") != null) {
        jvmArgs("-Dviaproxy.enableViaBedrock=false", "-Dviaproxy.enableViaLegacy=false", "-Dviaproxy.enableViaAprilFools=false")
    }
}
