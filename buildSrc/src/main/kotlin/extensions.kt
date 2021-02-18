import com.github.jengelman.gradle.plugins.shadow.tasks.ShadowJar
import com.github.jengelman.gradle.plugins.shadow.ShadowPlugin
import org.gradle.api.Project
import org.gradle.kotlin.dsl.apply
import org.gradle.kotlin.dsl.invoke
import org.gradle.kotlin.dsl.withType
import java.io.ByteArrayOutputStream

fun Project.configureShadowJar() {
    apply<ShadowPlugin>()
    tasks {
        withType<ShadowJar> {
            archiveClassifier.set("")
            archiveFileName.set("ViaBackwards-${project.name.substringAfter("viabackwards-").capitalize()}-${project.version}.jar")
            destinationDirectory.set(rootProject.projectDir.resolve("build/libs"))
            configureRelocations()
        }
        getByName("build") {
            dependsOn(withType<ShadowJar>())
        }
    }
}

private fun ShadowJar.configureRelocations() {
}

fun Project.latestCommitHash(): String {
    val byteOut = ByteArrayOutputStream()
    exec {
        commandLine = listOf("git", "rev-parse", "--short", "HEAD")
        standardOutput = byteOut
    }
    return byteOut.toString(Charsets.UTF_8.name()).trim()
}
