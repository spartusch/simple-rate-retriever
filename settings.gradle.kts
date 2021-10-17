pluginManagement {
    val kotlinVersion = "1.5.31"

    plugins {
        kotlin("jvm") version kotlinVersion
        kotlin("plugin.spring") version kotlinVersion
        kotlin("plugin.serialization") version kotlinVersion

        id("org.springframework.boot") version "2.5.5"
        id("io.spring.dependency-management") version "1.0.11.RELEASE"
        id("org.openapi.generator") version "5.2.1"
        id("com.gorylenko.gradle-git-properties") version "2.3.1"
        id("io.gitlab.arturbosch.detekt") version "1.18.1"
    }
}

rootProject.name = "simple-rate-retriever"