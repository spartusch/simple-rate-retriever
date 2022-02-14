
plugins {
    kotlin("jvm")
    kotlin("plugin.spring")
    kotlin("plugin.serialization")

    id("org.springframework.boot")
    id("io.spring.dependency-management")
    id("org.openapi.generator")
    id("com.gorylenko.gradle-git-properties")
    id("io.gitlab.arturbosch.detekt")
}

val jvmTarget = "17"

group = "com.github.spartusch"
version = "2.2.0"

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
    implementation("org.springframework.boot:spring-boot-starter-security")
    implementation("javax.validation:validation-api:latest.release")
    implementation("org.javamoney:moneta:latest.release")
    implementation("org.springframework.retry:spring-retry")

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
    testImplementation("org.springframework.security:spring-security-test")
    testImplementation("io.mockk:mockk:latest.release")
    testImplementation("com.github.tomakehurst:wiremock-standalone:latest.release")
    testImplementation("com.github.JensPiegsa:wiremock-extension:latest.release")
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
    apiPackage.set("com.github.spartusch.rateretriever.infrastructure.api.generated")
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
