plugins {
    id("me.modmuss50.mod-publish-plugin") version "2.2.1"
    id("io.papermc.hangar-publish-plugin") version "0.1.4"

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
    val mcVersionRange = property("mcVersionRange") as String
    val (mcVersionStart, mcVersionEnd) = mcVersionRange.split("-", limit = 2)
    publishMods {
        file = tasks.shadowJar.flatMap { it.archiveFile }
        type = if (isRelease) STABLE else if (isMainBranch) BETA else ALPHA
        version = suffixedVersion
        displayName = suffixedVersion
        changelog = changelogContent

        val modrinthOptions = modrinthOptions {
            accessToken = providers.environmentVariable("MODRINTH_TOKEN")
            projectId = "viabackwards"
            requires("viaversion")
            optional("viafabric", "viarewind")
        }
        modrinth {
            from(modrinthOptions)
            modLoaders.addAll("paper", "folia", "velocity")
            minecraftVersionRange {
                start = mcVersionStart
                end = mcVersionEnd
            }
        }
        // Fabric requires 1.14+ (18w43b), so it is uploaded separately with only the supported versions...
        modrinth("modrinthFabric") {
            from(modrinthOptions)
            modLoaders.add("fabric")
            minecraftVersionRange {
                start = "1.14"
                end = mcVersionEnd
            }
        }
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
                    platformVersions = listOf(mcVersionRange)
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
}
