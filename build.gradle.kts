plugins {
    val kotlinVersion = "1.4.0"

    kotlin("jvm") version kotlinVersion
    kotlin("plugin.spring") version kotlinVersion
    id("org.springframework.boot") version "2.3.3.RELEASE"
    id("io.spring.dependency-management") version "1.0.10.RELEASE"

    id("com.gorylenko.gradle-git-properties") version "2.2.3"
    id("io.gitlab.arturbosch.detekt") version "1.12.0"
}

val jvmTarget = "11"

group = "com.github.spartusch"
version = "2.0.0-SNAPSHOT"

repositories {
    mavenLocal()
    mavenCentral()
    jcenter() // required for detekt
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

    implementation("com.github.spartusch:excel-web-query:2.+")

    // Monitoring
    implementation("io.micrometer:micrometer-registry-prometheus")
    implementation("org.springframework.boot:spring-boot-starter-actuator")
    implementation("de.codecentric:spring-boot-admin-starter-client:2.+")

    testImplementation("org.springframework.boot:spring-boot-starter-test")
    testImplementation("com.github.tomakehurst:wiremock-standalone:2.+")
    testImplementation("com.github.JensPiegsa:wiremock-extension:0.4.+")

    detektPlugins("io.gitlab.arturbosch.detekt:detekt-formatting:1.+")
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
