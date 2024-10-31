plugins {
    base
    id("vb.build-logic")
}

allprojects {
    group = "com.viaversion"
    version = property("projectVersion") as String // from gradle.properties
    description = "Allows the connection of older clients to newer server versions for Minecraft servers."
}

val main = setOf(
    projects.viabackwards,
    projects.viabackwardsCommon,
    projects.viabackwardsBukkit,
    projects.viabackwardsVelocity,
    projects.viabackwardsSponge,
    projects.viabackwardsFabric
).map { it.dependencyProject }

subprojects {
    when (this) {
        in main -> plugins.apply("vb.shadow-conventions")
        else -> plugins.apply("vb.base-conventions")
    }
}
