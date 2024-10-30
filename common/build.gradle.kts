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
tasks.register<JavaExec>("runViaProxy") {
    dependsOn(tasks.shadowJar)

    val viaProxyConfiguration = configurations.create("viaProxy")
    viaProxyConfiguration.dependencies.add(dependencies.create(rootProject.libs.viaProxy.get().copy().setTransitive(false)))

    mainClass.set("net.raphimc.viaproxy.ViaProxy")
    classpath = viaProxyConfiguration
    workingDir = file("run")
    jvmArgs = listOf("-DskipUpdateCheck")

    if (System.getProperty("viaproxy.gui.autoStart") != null) {
        jvmArgs("-Dviaproxy.gui.autoStart")
    }

    doFirst {
        val jarsDir = file("$workingDir/jars")
        jarsDir.mkdirs()
        file("$jarsDir/${project.name}.jar").writeBytes(tasks.shadowJar.get().archiveFile.get().asFile.readBytes())
    }

    doLast {
        file("$workingDir/jars/${project.name}.jar").delete()
        file("$workingDir/logs").deleteRecursively()
    }
}
