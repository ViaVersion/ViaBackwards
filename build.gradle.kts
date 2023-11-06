plugins {
    base
    id("vb.build-logic")
}

allprojects {
    group = "com.viaversion"
    version = property("projectVersion") as String // from gradle.properties
    description = "Allow older clients to join newer server versions."
}

val main = setOf(
        projects.viabackwards,
        projects.viabackwardsCommon,
        projects.viabackwardsBukkit,
        projects.viabackwardsBungee,
        projects.viabackwardsFabric,
        projects.viabackwardsSponge,
        projects.viabackwardsVelocity
).map { it.dependencyProject }

subprojects {
    when (this) {
        in main -> plugins.apply("vb.shadow-conventions")
        else -> plugins.apply("vb.standard-conventions")
    }
}
