plugins {
    val kotlinVersion = "1.4.20"

    kotlin("jvm") version kotlinVersion
    kotlin("plugin.spring") version kotlinVersion
    kotlin("plugin.serialization") version kotlinVersion

    id("org.springframework.boot") version "2.3.5.RELEASE"
    id("io.spring.dependency-management") version "1.0.10.RELEASE"
    id("com.gorylenko.gradle-git-properties") version "2.2.4"
    id("io.gitlab.arturbosch.detekt") version "1.14.2"
}

val jvmTarget = "11"

group = "com.github.spartusch"
version = "2.0.0-SNAPSHOT"

repositories {
    mavenLocal()
    mavenCentral()
    jcenter() // required for detekt, kotlinx-serialization
    maven { url = uri("https://jitpack.io") } // required for com.github.JensPiegsa:wiremock-extension
}

configurations
    .filter { it.name.endsWith("compileClasspath", ignoreCase = true) || it.name == "detektPlugins" }
    .forEach { it.resolutionStrategy.activateDependencyLocking() }

dependencies {
    annotationProcessor("org.springframework.boot:spring-boot-configuration-processor")

    implementation(kotlin("stdlib-jdk8"))
    implementation("org.springframework.boot:spring-boot-starter-web")
    implementation("com.fasterxml.jackson.module:jackson-module-kotlin")
    implementation("org.jetbrains.kotlinx:kotlinx-serialization-json:latest.release")

    implementation("com.github.spartusch:excel-web-query:+")

    // Monitoring
    implementation("io.micrometer:micrometer-registry-prometheus")
    implementation("org.springframework.boot:spring-boot-starter-actuator")
    implementation("de.codecentric:spring-boot-admin-starter-client:latest.release")

    testImplementation("org.springframework.boot:spring-boot-starter-test")
    testImplementation("com.github.tomakehurst:wiremock-standalone:latest.release")
    testImplementation("com.github.JensPiegsa:wiremock-extension:latest.release")

    detektPlugins("io.gitlab.arturbosch.detekt:detekt-formatting:latest.release")
}

springBoot {
    buildInfo()
}

detekt {
    buildUponDefaultConfig = true
}

tasks {
    withType<org.jetbrains.kotlin.gradle.tasks.KotlinCompile> {
        kotlinOptions.jvmTarget = jvmTarget
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
