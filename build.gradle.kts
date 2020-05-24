
plugins {
    val kotlinVersion = "1.3.72"

    kotlin("jvm") version kotlinVersion
    id("org.jetbrains.kotlin.plugin.spring") version kotlinVersion
    id("org.springframework.boot") version "2.3.0.RELEASE"
    id("com.gorylenko.gradle-git-properties") version "2.2.2"
}

apply(plugin = "io.spring.dependency-management")

object Versions {
    object Dependencies {
        const val excelWebQuery = "2.0.0-SNAPSHOT"
        const val springBootAdmin = "2.2.3"
        const val wiremock = "2.26.3"
        const val wiremockExtension = "0.4.0"
    }

    const val jvmTarget = "1.8"
    const val projectVersion = "2.0.0-SNAPSHOT"
}

repositories {
    mavenLocal()
    mavenCentral()
    maven { url = uri("https://jitpack.io") }
    maven { url = uri("https://spartusch.github.io/mvn-repo") }
}

dependencies {
    annotationProcessor("org.springframework.boot:spring-boot-configuration-processor")

    implementation(kotlin("stdlib-jdk8"))
    implementation("org.springframework.boot:spring-boot-starter-web")
    implementation("com.fasterxml.jackson.module:jackson-module-kotlin")

    implementation("com.github.spartusch:excel-web-query:${Versions.Dependencies.excelWebQuery}")

    // Monitoring
    implementation("io.micrometer:micrometer-registry-prometheus")
    implementation("org.springframework.boot:spring-boot-starter-actuator")
    implementation("de.codecentric:spring-boot-admin-starter-client:${Versions.Dependencies.springBootAdmin}")

    testImplementation("org.springframework.boot:spring-boot-starter-test")
    testImplementation("com.github.tomakehurst:wiremock-standalone:${Versions.Dependencies.wiremock}")
    testImplementation("com.github.JensPiegsa:wiremock-extension:${Versions.Dependencies.wiremockExtension}")
}

group = "com.github.spartusch"
version = Versions.projectVersion

springBoot {
    buildInfo()
}

tasks {
    withType<org.jetbrains.kotlin.gradle.tasks.KotlinCompile>() {
        kotlinOptions.jvmTarget = Versions.jvmTarget
    }

    bootJar {
        // Setting launchScript breaks bootBuildImage
        if (!project.gradle.startParameter.taskNames.contains("bootBuildImage")) {
            launchScript()
        }
        layered()
    }

    bootBuildImage {
        imageName = "${project.group}/${project.name}:latest"
    }

    test {
        useJUnitPlatform()
        testLogging.events("started", "skipped", "failed")
    }
}
