pluginManagement {
    val kotlinVersion = "1.7.10"

    plugins {
        kotlin("jvm") version kotlinVersion
        kotlin("plugin.spring") version kotlinVersion
        kotlin("plugin.serialization") version kotlinVersion

        id("org.springframework.boot") version "2.7.4"
        id("io.spring.dependency-management") version "1.0.14.RELEASE"
        id("org.openapi.generator") version "6.0.1"
        id("com.gorylenko.gradle-git-properties") version "2.4.1"
        id("io.gitlab.arturbosch.detekt") version "1.21.0"
    }
}

rootProject.name = "simple-rate-retriever"