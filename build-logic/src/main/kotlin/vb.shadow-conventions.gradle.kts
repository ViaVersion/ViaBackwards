import com.github.jengelman.gradle.plugins.shadow.tasks.ShadowJar
import org.gradle.jvm.tasks.Jar
import org.gradle.kotlin.dsl.named

plugins {
    id("vb.base-conventions")
    id("maven-publish")
    id("com.gradleup.shadow")
}

tasks {
    named<Jar>("jar") {
        archiveClassifier.set("unshaded")
        from(project.rootProject.file("LICENSE"))
    }
    val shadowJar = named<ShadowJar>("shadowJar") {
        duplicatesStrategy = DuplicatesStrategy.EXCLUDE
        archiveClassifier.set("")
        configureRelocations()
    }
    named("build") {
        dependsOn(shadowJar)
    }
}

publishing {
    publications.create<MavenPublication>("mavenJava") {
        groupId = rootProject.group as String
        artifactId = project.name
        version = rootProject.version as String

        artifact(tasks["shadowJar"])
        artifact(tasks["sourcesJar"])
    }
    repositories.maven {
        name = "Via"
        url = uri("https://repo.viaversion.com/")
        credentials(PasswordCredentials::class)
        authentication {
            create<BasicAuthentication>("basic")
        }
    }
}

fun ShadowJar.configureRelocations() {
    relocate("com.google.gson", "com.viaversion.viaversion.libs.gson")
    relocate("it.unimi.dsi.fastutil", "com.viaversion.viaversion.libs.fastutil")
}
