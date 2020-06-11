
plugins {
    val kotlinVersion = "1.3.72"

    kotlin("jvm") version kotlinVersion
    id("org.jetbrains.kotlin.plugin.spring") version kotlinVersion
    id("org.springframework.boot") version "2.3.0.RELEASE"
    id("io.spring.dependency-management") version "1.0.9.RELEASE"

    id("com.gorylenko.gradle-git-properties") version "2.2.2"
    id("io.gitlab.arturbosch.detekt") version "1.9.1"
}

object Versions {
    object Dependencies {
        const val excelWebQuery = "2.0.0-SNAPSHOT"
        const val springBootAdmin = "2.2.3"
        const val detektVersion = "1.9.1"
        const val wiremock = "2.26.3"
        const val wiremockExtension = "0.4.0"
    }
    const val jvmTarget = "11"
    const val projectVersion = "2.0.0-SNAPSHOT"
}

repositories {
    mavenLocal()
    mavenCentral()
    jcenter() // required for detekt
    maven { url = uri("https://jitpack.io") } // required for com.github.JensPiegsa:wiremock-extension
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

    detektPlugins("io.gitlab.arturbosch.detekt:detekt-formatting:${Versions.Dependencies.detektVersion}")
}

group = "com.github.spartusch"
version = Versions.projectVersion

springBoot {
    buildInfo()
}

detekt {
    buildUponDefaultConfig = true
}

tasks {
    withType<org.jetbrains.kotlin.gradle.tasks.KotlinCompile> {
        kotlinOptions.jvmTarget = Versions.jvmTarget
    }

    test {
        useJUnitPlatform()
        testLogging.events("started", "skipped", "failed")
    }

    bootJar {
        if (project.hasProperty("generateLaunchScript")) {
            launchScript()
        }
        layered()
    }

    register<Exec>("dockerImage") {
        val name = "${project.group}/${project.name}"
        commandLine = listOf("docker", "image", "build", "-t", "$name:${project.version}", "-t", "$name:latest", ".")
    }
}
