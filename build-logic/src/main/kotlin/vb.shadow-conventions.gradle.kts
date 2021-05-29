import com.github.jengelman.gradle.plugins.shadow.tasks.ShadowJar
import org.gradle.jvm.tasks.Jar
import org.gradle.kotlin.dsl.named

plugins {
    id("vb.base-conventions")
    id("com.github.johnrengelman.shadow")
}

tasks {
    named<Jar>("jar") {
        archiveClassifier.set("unshaded")
        from(project.rootProject.file("LICENSE"))
    }
    val shadowJar = named<ShadowJar>("shadowJar") {
        archiveClassifier.set("")
        configureRelocations()
    }
    named("build") {
        dependsOn(shadowJar)
    }
}

publishShadowJar()

fun ShadowJar.configureRelocations() {
    relocate("com.google.gson", "com.viaversion.viaversion.libs.gson")
    relocate("it.unimi.dsi.fastutil", "com.viaversion.viaversion.libs.fastutil")
}
