plugins {
    id("java")
    id("org.jetbrains.kotlin.jvm") version "1.9.21"
    id("org.jetbrains.intellij") version "1.16.1"
}

group = "com.github.ignaciotcrespo.vectordrawablethumbnailsplugin"
version = "1.0.5"

repositories {
    mavenCentral()
    google()
}

dependencies {
    implementation("io.reactivex.rxjava2:rxjava:2.2.21")
    implementation("com.android.tools:sdk-common:31.2.2")
}

// Configure Gradle IntelliJ Plugin
// Read more: https://plugins.jetbrains.com/docs/intellij/tools-gradle-intellij-plugin.html
intellij {
    version.set("2023.1.5")
    type.set("IC") // Target IDE Platform

    // Plugin Dependencies. Uses `platformPlugins` property from the gradle.properties file.
    plugins.set("com.intellij.java, org.intellij.intelliLang, org.jetbrains.kotlin, org.jetbrains.idea.maven"
        .split(',').map(String::trim).filter(String::isNotEmpty).toList()
    )
}

tasks {
    // Set the JVM compatibility versions
    withType<JavaCompile> {
        sourceCompatibility = "17"
        targetCompatibility = "17"
    }
    withType<org.jetbrains.kotlin.gradle.tasks.KotlinCompile> {
        kotlinOptions.jvmTarget = "17"
    }

    patchPluginXml {
        sinceBuild.set("231")
        untilBuild.set("241.*")
    }

    signPlugin {
        certificateChain.set(System.getenv("CERTIFICATE_CHAIN"))
        privateKey.set(System.getenv("PRIVATE_KEY"))
        password.set(System.getenv("PRIVATE_KEY_PASSWORD"))
    }

    publishPlugin {
        token.set(System.getenv("PUBLISH_TOKEN"))
    }
}
