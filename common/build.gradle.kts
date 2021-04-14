plugins {
    id("net.kyori.blossom")
}

blossom {
    replaceToken("\$VERSION", project.version)
    replaceToken("\$IMPL_VERSION", "git-ViaBackwards-${project.version}:${rootProject.latestCommitHash()}")
}

dependencies {
    compileOnlyApi(libs.viaver)
    compileOnlyApi(libs.netty)
    compileOnlyApi(libs.guava)
    compileOnlyApi(libs.checkerQual)
}