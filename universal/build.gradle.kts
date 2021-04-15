import com.github.jengelman.gradle.plugins.shadow.tasks.ShadowJar

tasks {
    withType<ShadowJar> {
        archiveClassifier.set("")
        archiveFileName.set("ViaBackwards-${project.version}.jar")
        destinationDirectory.set(rootProject.projectDir.resolve("build/libs"))
        duplicatesStrategy = DuplicatesStrategy.EXCLUDE
        sequenceOf(
                rootProject.projects.viabackwardsBukkit,
                rootProject.projects.viabackwardsBungee,
                rootProject.projects.viabackwardsFabric,
                rootProject.projects.viabackwardsSponge,
                rootProject.projects.viabackwardsVelocity,
        ).map { it.dependencyProject }.forEach { subproject ->
            val shadowJarTask = subproject.tasks.getByName("shadowJar", ShadowJar::class)
            dependsOn(shadowJarTask)
            dependsOn(subproject.tasks.withType<Jar>())
            from(zipTree(shadowJarTask.archiveFile))
        }
    }
    build {
        dependsOn(withType<ShadowJar>())
    }
}
