dependencies {
    implementation(projects.viabackwardsCommon)
    compileOnly(libs.velocity) {
        // Requires Java 11
        exclude("com.velocitypowered", "velocity-brigadier")
    }
    annotationProcessor(libs.velocity)
}
