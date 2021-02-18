dependencies {
    implementation(project(":viabackwards-common"))
    compileOnly("org.spigotmc", "spigot-api", Versions.spigot) {
        exclude("com.google.code.gson", "gson")
        exclude("javax.persistence", "persistence-api")
    }
}
