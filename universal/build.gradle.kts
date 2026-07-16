plugins {
    id("io.papermc.hangar-publish-plugin") version "0.1.4"
    id("com.modrinth.minotaur") version "2.+"

    // A nice no-conflict comment for patching in downgrading
}

dependencies {
    api(projects.viabackwardsCommon)
    api(projects.viabackwardsBukkit)
    api(projects.viabackwardsVelocity)
    api(projects.viabackwardsFabric)
}

tasks {
    shadowJar {
        manifest {
            attributes["paperweight-mappings-namespace"] = "mojang"
        }
        archiveFileName.set("ViaBackwards-${project.version}.jar")
        destinationDirectory.set(rootProject.projectDir.resolve("build/libs"))
    }
    sourcesJar {
        duplicatesStrategy = DuplicatesStrategy.EXCLUDE
        rootProject.subprojects.forEach { subproject ->
            if (subproject == project) return@forEach
            val sourceSets = subproject.extensions.findByType(SourceSetContainer::class) ?: return@forEach
            from(sourceSets.named("main").map { it.allSource })
        }
    }
}

val runNumber: String? = System.getenv("GITHUB_RUN_NUMBER")
val branch = if (runNumber != null) rootProject.branchName() else ""
val baseVersion = project.version as String
val isRelease = !baseVersion.contains('-')
val isMainBranch = branch == "master"
if (runNumber != null && (!isRelease || isMainBranch)) { // Only publish releases from the main branch
    val suffixedVersion = if (isRelease) baseVersion else "$baseVersion+$runNumber"
    val changelogContent = if (isRelease) {
        "See [GitHub](https://github.com/ViaVersion/ViaBackwards) for release notes."
    } else {
        val commitHash = rootProject.latestCommitHash()
        "[$commitHash](https://github.com/ViaVersion/ViaBackwards/commit/$commitHash) ${rootProject.latestCommitMessage()}"
    }
    modrinth {
        val mcVersions: List<String> = (property("mcVersions") as String)
            .split(",")
            .map { it.trim() }
        token.set(System.getenv("MODRINTH_TOKEN"))
        projectId.set("viabackwards")
        versionType.set(if (isRelease) "release" else if (isMainBranch) "beta" else "alpha")
        versionNumber.set(suffixedVersion)
        versionName.set(suffixedVersion)
        changelog.set(changelogContent)
        uploadFile.set(tasks.shadowJar.flatMap { it.archiveFile })
        gameVersions.set(mcVersions)
        loaders.add("fabric")
        loaders.add("paper")
        loaders.add("folia")
        loaders.add("velocity")
        autoAddDependsOn.set(false)
        detectLoaders.set(false)
        dependencies {
            required.project("viaversion")
            optional.project("viafabric")
            optional.project("viarewind")
        }
    }
    tasks.modrinth {
        notCompatibleWithConfigurationCache("")
    }

    hangarPublish {
        publications.register("plugin") {
            version = suffixedVersion
            id = "ViaBackwards"
            channel = if (isRelease) "Release" else if (isMainBranch) "Snapshot" else "Alpha"
            changelog = changelogContent
            apiKey = System.getenv("HANGAR_TOKEN")
            platforms {
                paper {
                    jar = tasks.shadowJar.flatMap { it.archiveFile }
                    platformVersions = listOf(property("mcVersionRange") as String)
                    dependencies {
                        hangar("ViaVersion") {
                            required = true
                        }
                        hangar("ViaRewind") {
                            required = false
                        }
                    }
                }
                velocity {
                    jar = tasks.shadowJar.flatMap { it.archiveFile }
                    platformVersions = listOf(property("velocityVersion") as String)
                    dependencies {
                        hangar("ViaVersion") {
                            required = true
                        }
                        hangar("ViaRewind") {
                            required = false
                        }
                    }
                }
            }
        }
    }
    tasks.named("publishPluginPublicationToHangar") {
        notCompatibleWithConfigurationCache("")
    }
}
