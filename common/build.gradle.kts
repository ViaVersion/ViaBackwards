plugins {
    id("net.kyori.blossom") version "1.2.0"
}

blossom {
    replaceToken("\$VERSION", project.version)
    replaceToken("\$IMPL_VERSION", "git-ViaBackwards-${project.version}:${rootProject.latestCommitHash()}")
}

dependencies {
    compileOnly("io.netty", "netty-all", Versions.netty)
    compileOnlyApi("us.myles", "viaversion", Versions.viaversion)
    compileOnlyApi("com.google.guava", "guava", Versions.guava)
    compileOnlyApi("org.checkerframework", "checker-qual", Versions.checkerQual)
}