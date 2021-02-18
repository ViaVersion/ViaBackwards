rootProject.name = "viabackwards-parent"

setupViaSubproject("common")
setupViaSubproject("bukkit")
setupViaSubproject("bungee")
setupViaSubproject("velocity")
setupViaSubproject("sponge")
setupViaSubproject("fabric")
setupViaSubproject("universal")

fun setupViaSubproject(name: String) {
    setupSubproject("viabackwards-$name") {
        projectDir = file(name)
    }
}

inline fun setupSubproject(name: String, block: ProjectDescriptor.() -> Unit) {
    include(name)
    project(":$name").apply(block)
}
