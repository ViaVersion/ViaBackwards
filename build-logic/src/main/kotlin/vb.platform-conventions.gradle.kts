plugins {
    id("vb.shadow-conventions")
}

tasks {
    shadowJar {
        archiveFileName.set("ViaBackwards-${project.name.substringAfter("viabackwards-").capitalize()}-${project.version}.jar")
        destinationDirectory.set(rootProject.layout.buildDirectory.dir("libs"))
    }
}
