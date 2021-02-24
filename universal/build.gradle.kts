import com.github.jengelman.gradle.plugins.shadow.tasks.ShadowJar

tasks {
    withType<ShadowJar> {
        archiveClassifier.set("")
        archiveFileName.set("ViaBackwards-${project.version}.jar")
        destinationDirectory.set(rootProject.projectDir.resolve("build/libs"))
        duplicatesStrategy = DuplicatesStrategy.EXCLUDE
        arrayOf(
                "bukkit",
                "bungee",
                "sponge",
                "velocity",
                "fabric"
        ).forEach {
            val subProject = rootProject.project(":viabackwards-$it")
            val shadowJarTask = subProject.tasks.getByName("shadowJar")
            from(zipTree(shadowJarTask.outputs.files.singleFile))
            dependsOn(shadowJarTask)
        }
    }
    build {
        dependsOn(withType<ShadowJar>())
    }
}
