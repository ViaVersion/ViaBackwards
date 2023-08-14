import org.gradle.api.JavaVersion
import org.gradle.api.Project
import org.gradle.api.plugins.JavaPluginExtension
import org.gradle.api.publish.PublishingExtension
import org.gradle.api.publish.maven.MavenPublication
import org.gradle.kotlin.dsl.configure
import org.gradle.kotlin.dsl.get
import org.gradle.kotlin.dsl.named
import java.io.ByteArrayOutputStream

fun Project.publishShadowJar() {
    configurePublication {
        artifact(tasks["shadowJar"])
        artifact(tasks["sourcesJar"])
    }
}

fun Project.publishJavaComponents() {
    configurePublication {
        from(components["java"])
    }
}

private fun Project.configurePublication(configurer: MavenPublication.() -> Unit) {
    extensions.configure<PublishingExtension> {
        publications.named<MavenPublication>("mavenJava") {
            apply(configurer)
        }
    }
}

fun Project.latestCommitHash(): String {
    val byteOut = ByteArrayOutputStream()
    exec {
        commandLine = listOf("git", "rev-parse", "--short", "HEAD")
        standardOutput = byteOut
    }
    return byteOut.toString(Charsets.UTF_8.name()).trim()
}

fun Project.latestCommitMessage(): String {
    val byteOut = ByteArrayOutputStream()
    exec {
        commandLine = listOf("git", "log", "-1", "--pretty=%B")
        standardOutput = byteOut
    }
    return byteOut.toString(Charsets.UTF_8.name()).trim()
}

fun Project.branchName(): String {
    val byteOut = ByteArrayOutputStream()
    exec {
        commandLine = listOf("git", "branch")
        standardOutput = byteOut
    }
    return byteOut.toString(Charsets.UTF_8.name()).trim()
}

fun Project.parseMinecraftSnapshotVersion(version: String): String? {
    val separatorIndex = version.indexOf('-')
    val lastSeparatorIndex = version.lastIndexOf('-')
    if (separatorIndex == -1 || separatorIndex == lastSeparatorIndex) {
        return null
    }
    return version.substring(separatorIndex + 1, lastSeparatorIndex)
}

fun JavaPluginExtension.javaTarget(version: Int) {
    sourceCompatibility = JavaVersion.toVersion(version)
    targetCompatibility = JavaVersion.toVersion(version)
}
