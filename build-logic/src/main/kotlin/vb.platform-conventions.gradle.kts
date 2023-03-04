import java.util.*

plugins {
    id("vb.shadow-conventions")
}

tasks {
    shadowJar {
        archiveFileName.set("ViaBackwards-${project.name.substringAfter("viabackwards-").replaceFirstChar { if (it.isLowerCase()) it.titlecase(Locale.getDefault()) else it.toString() }}-${project.version}.jar")
        destinationDirectory.set(rootProject.layout.buildDirectory.dir("libs"))
    }
}
