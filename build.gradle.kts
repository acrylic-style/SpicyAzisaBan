import java.io.ByteArrayOutputStream

plugins {
    kotlin("jvm") version "2.0.21"
    id("com.gradleup.shadow") version "8.3.3"
    `maven-publish`
    `java-library`
}

group = "net.azisaba.spicyazisaban"
version = "2.0.0-${getBranch()}-${getGitHash()}${if (hasUncommittedChanges()) "-debug" else ""}"

java {
    toolchain.languageVersion.set(JavaLanguageVersion.of(17))
    withJavadocJar()
    withSourcesJar()
}

fun getBranch(): String =
    file("./.git/HEAD")
        .readText()
        .replace("^.*: (.*)$".toRegex(), "$1")
        .trim(' ', '\n')
        .split('/')
        .last()

fun getGitHash(): String {
    return try {
        val stdout = ByteArrayOutputStream()
        exec {
            commandLine("git", "rev-parse", "--short", "HEAD")
            standardOutput = stdout
        }
        stdout.toString().trim()
    } catch (_: Exception) {
        val ref = file("./.git/HEAD").readText().replace("^.*: (.*)$".toRegex(), "$1").trim(' ', '\n')
        println("Reading file ${file("./.git/$ref").absolutePath}")
        file("./.git/$ref").readText().trim(' ', '\n').substring(0..7)
    }
}

fun hasUncommittedChanges(): Boolean {
    return try {
        val stdout = ByteArrayOutputStream()
        exec {
            commandLine("git", "status", "--porcelain")
            standardOutput = stdout
        }
        stdout.toString().trim().isNotBlank()
    } catch (_: Exception) {
        false
    }
}

val javaComponent = components["java"] as AdhocComponentWithVariants
javaComponent.withVariantsFromConfiguration(configurations["sourcesElements"]) {
    skip()
}

repositories {
    // mavenLocal()
    mavenCentral()
    maven { url = uri("https://oss.sonatype.org/content/repositories/snapshots/") }
    maven { url = uri("https://repo.azisaba.net/repository/maven-public/") }
}

subprojects {
    group = parent!!.group
    version = parent!!.version

    repositories {
        // mavenLocal()
        mavenCentral()
        maven { url = uri("https://oss.sonatype.org/content/repositories/snapshots/") }
        maven { url = uri("https://repo.azisaba.net/repository/maven-public/") }
    }

    apply {
        plugin("org.jetbrains.kotlin.jvm")
        plugin("com.gradleup.shadow")
        plugin("maven-publish")
        plugin("java-library")
    }

    java {
        toolchain.languageVersion.set(JavaLanguageVersion.of(17))
        withJavadocJar()
        withSourcesJar()
    }

    tasks {
        test {
            useJUnitPlatform()
        }

        processResources {
            doNotTrackState("bungee.yml should be updated every time")
            filteringCharset = "UTF-8"
            from(sourceSets.main.get().resources.srcDirs) {
                include("**")

                val tokenReplacementMap = mapOf(
                    "version" to project.version,
                    "name" to project.rootProject.name,
                    "debugBuild" to hasUncommittedChanges().toString(),
                    "devBuild" to (getBranch() != "main").toString(),
                )

                filter<org.apache.tools.ant.filters.ReplaceTokens>("tokens" to tokenReplacementMap)
            }

            duplicatesStrategy = DuplicatesStrategy.INCLUDE

            from(projectDir) { include("LICENSE") }
        }

        shadowJar {
            if (project.name != "cli") {
                dependencies {
                    exclude(dependency("com.google.guava:guava:.*"))
                    exclude(dependency("org.reflections:reflections:.*"))
                    exclude(dependency("org.json:json:.*"))
                    exclude(dependency("org.yaml:snakeyaml:.*"))
                    exclude(dependency("com.google.code.findbugs:jsr305:.*"))
                    exclude(dependency("org.javassist:javassist:.*"))
                    exclude(dependency("org.slf4j:slf4j-api:.*"))
                    exclude(dependency("com.google.guava:failureaccess:.*"))
                    exclude(dependency("com.google.guava:listenablefuture:.*"))
                    exclude(dependency("com.google.code.findbugs:jsr305:.*"))
                    exclude(dependency("org.checkerframework:checker-qual:.*"))
                    exclude(dependency("com.google.errorprone:error_prone_annotations:.*"))
                    exclude(dependency("com.google.j2objc:j2objc-annotations:.*"))
                    exclude(dependency("org.jetbrains.kotlin:kotlin-stdlib-jdk8:.*"))
                    exclude(dependency("org.xerial.snappy:snappy-java:"))
                    exclude(dependency("org.jetbrains:annotations:.*"))
                }
            }
            relocate("kotlin", "net.azisaba.spicyAzisaBan.libs.kotlin")
            relocate("util", "net.azisaba.spicyAzisaBan.libs.util")
            relocate("xyz.acrylicstyle.sql", "net.azisaba.spicyAzisaBan.libs.xyz.acrylicstyle.sql")
            relocate("xyz.acrylicstyle.mcutil", "net.azisaba.spicyAzisaBan.libs.xyz.acrylicstyle.mcutil")
            relocate("org.mariadb", "net.azisaba.spicyAzisaBan.libs.org.mariadb")
            relocate("org.objectweb", "net.azisaba.spicyAzisaBan.libs.org.objectweb")
            relocate("org.json", "net.azisaba.spicyAzisaBan.libs.org.json")
            relocate("com.google.guava", "net.azisaba.spicyAzisaBan.libs.com.google.guava")
            archiveFileName.set("SpicyAzisaBan-${project.version}.jar")
        }
    }

    dependencies {
        testImplementation("org.junit.jupiter:junit-jupiter:5.8.1")
    }
}

allprojects {
    publishing {
        repositories {
            maven {
                name = "repo"
                credentials(PasswordCredentials::class)
                url = uri(
                    if (project.version.toString().endsWith("SNAPSHOT"))
                        project.findProperty("deploySnapshotURL") ?: System.getProperty("deploySnapshotURL", "https://repo.azisaba.net/repository/maven-snapshots/")
                    else
                        project.findProperty("deployReleasesURL") ?: System.getProperty("deployReleasesURL", "https://repo.azisaba.net/repository/maven-releases/")
                )
            }
        }

        publications {
            create<MavenPublication>("mavenJava") {
                //from(components["java"])
                //artifact(tasks.withType<GenerateModuleMetadata>())
                artifact(tasks["jar"])
                artifact(tasks["sourcesJar"])
                artifact(tasks["javadocJar"])
            }
        }
    }
}

println("Version: ${project.version}")
println("Debug build: ${hasUncommittedChanges()}")
println("Dev build: ${project.version.toString().contains("-dev")}")
