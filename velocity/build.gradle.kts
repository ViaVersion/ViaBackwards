dependencies {
    implementation(project(":viabackwards-common"))
    compileOnly("com.velocitypowered", "velocity-api", Versions.velocityApi)
    annotationProcessor("com.velocitypowered", "velocity-api", Versions.velocityApi)
}
