dependencies {
    implementation(project(":viabackwards-common"))
    compileOnly("com.velocitypowered", "velocity-api", Versions.velocity)
    annotationProcessor("com.velocitypowered", "velocity-api", Versions.velocity)
}
