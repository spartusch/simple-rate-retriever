plugins {
    val kotlinVersion = "1.5.10"

    kotlin("jvm") version kotlinVersion
    kotlin("plugin.spring") version kotlinVersion
    kotlin("plugin.serialization") version kotlinVersion

    id("org.springframework.boot") version "2.5.0"
    id("io.spring.dependency-management") version "1.0.11.RELEASE"
    id("org.openapi.generator") version "5.1.1"
    id("com.gorylenko.gradle-git-properties") version "2.3.1"
    id("io.gitlab.arturbosch.detekt") version "1.17.1"
}

val jvmTarget = "11"

group = "com.github.spartusch"
version = "2.1.0-SNAPSHOT"

repositories {
    mavenCentral()
    maven {
        url = uri("https://jitpack.io") // required for wiremock-extension, excel-web-query
    }
}

dependencyLocking {
    lockAllConfigurations()
}

dependencies {
    annotationProcessor("org.springframework.boot:spring-boot-configuration-processor")

    implementation("org.springframework.boot:spring-boot-starter-web")
    implementation("javax.validation:validation-api:latest.release")
    implementation("org.javamoney:moneta:latest.release")

    implementation("com.github.spartusch:excel-web-query:latest.release")

    // Kotlin Support
    implementation(kotlin("stdlib-jdk8"))
    implementation("com.fasterxml.jackson.module:jackson-module-kotlin")
    implementation("org.jetbrains.kotlinx:kotlinx-serialization-json:latest.release")

    // Monitoring
    implementation("io.micrometer:micrometer-registry-prometheus")
    implementation("org.springframework.boot:spring-boot-starter-actuator")
    implementation("de.codecentric:spring-boot-admin-starter-client:latest.release")

    testImplementation("org.springframework.boot:spring-boot-starter-test")
    testImplementation("io.mockk:mockk:latest.release")
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

allOpen {
    annotation("org.springframework.boot.context.properties.ConfigurationProperties")
}

openApiGenerate {
    generatorName.set("kotlin-spring")
    inputSpec.set("$projectDir/src/main/resources/static/openapi.yml")
    outputDir.set("$buildDir/generated-sources")
    apiPackage.set("com.github.spartusch.rateretriever.rate.v1.controller.generated")
    globalProperties.set(mapOf("apis" to ""))
    additionalProperties.set(mapOf("delegatePattern" to "true"))
}

sourceSets.getByName("main") {
    java.srcDir("$buildDir/generated-sources")
}

tasks {
    compileKotlin {
        dependsOn(openApiGenerate, detekt)
        kotlinOptions.jvmTarget = jvmTarget
    }

    test {
        useJUnitPlatform()
        testLogging.events("skipped", "failed")
    }

    bootJar {
        if (project.hasProperty("generateLaunchScript")) {
            launchScript()
        }
    }

    register<Exec>("dockerImage") {
        val name = "${project.group}/${project.name}"
        commandLine = listOf("docker", "image", "build", "-t", "$name:${project.version}", "-t", "$name:latest", ".")
    }
}
