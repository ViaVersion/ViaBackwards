import org.gradle.api.JavaVersion
import org.gradle.api.Project
import org.gradle.api.plugins.JavaPluginExtension
import java.io.ByteArrayOutputStream

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
        commandLine = listOf("git", "rev-parse", "--abbrev-ref", "HEAD")
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
