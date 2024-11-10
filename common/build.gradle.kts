dependencies {
    api("xyz.acrylicstyle.java-util:common:2.1.0")
    api("xyz.acrylicstyle.java-util:maven:2.1.0")
    api("net.blueberrymc:native-util:2.1.2")
    api("org.jetbrains.kotlin:kotlin-stdlib:2.0.21")
    api("xyz.acrylicstyle.util:all:0.16.6") {
        exclude("com.google.guava", "guava")
        exclude("org.reflections", "reflections")
        exclude("org.json", "json")
        exclude("org.yaml", "snakeyaml")
        exclude("xyz.acrylicstyle.util", "maven")
    }
    api("xyz.acrylicstyle:sequelize4j:0.6.3") {
        exclude("xyz.acrylicstyle", "java-util-all")
    }
    api("xyz.acrylicstyle:minecraft-util:1.0.0") {
        exclude("xyz.acrylicstyle", "java-util-all")
    }
    api("org.ow2.asm:asm:9.7.1")
    api("org.ow2.asm:asm-commons:9.7.1")
    api("net.kyori:adventure-text-minimessage:4.17.0")
    api("net.kyori:adventure-text-serializer-legacy:4.17.0")
    compileOnlyApi("org.mariadb.jdbc:mariadb-java-client:3.5.0")
}
