pluginManagement {
    val kotlinVersion = "1.6.0"

    plugins {
        kotlin("jvm") version kotlinVersion
        kotlin("plugin.spring") version kotlinVersion
        kotlin("plugin.serialization") version kotlinVersion

        id("org.springframework.boot") version "2.6.1"
        id("io.spring.dependency-management") version "1.0.11.RELEASE"
        id("org.openapi.generator") version "5.3.0"
        id("com.gorylenko.gradle-git-properties") version "2.3.2"
        id("io.gitlab.arturbosch.detekt") version "1.19.0"
    }
}

rootProject.name = "simple-rate-retriever"