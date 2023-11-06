dependencies {
    compileOnlyApi(projects.viabackwardsCommon)
    compileOnly(libs.paper) {
        exclude("com.google.code.gson", "gson")
        exclude("javax.persistence", "persistence-api")
    }
}
