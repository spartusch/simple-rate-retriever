pluginManagement {
    val kotlinVersion: String by settings
    val springBootVersion: String by settings
    val springDependencyManagementVersion: String by settings
    val openApiGeneratorVersion: String by settings
    val gradleGitPropertiesVersion: String by settings
    val detektVersion: String by settings

    plugins {
        kotlin("jvm") version kotlinVersion
        kotlin("plugin.spring") version kotlinVersion
        kotlin("plugin.serialization") version kotlinVersion

        id("org.springframework.boot") version springBootVersion
        id("io.spring.dependency-management") version springDependencyManagementVersion
        id("org.openapi.generator") version openApiGeneratorVersion
        id("com.gorylenko.gradle-git-properties") version gradleGitPropertiesVersion
        id("io.gitlab.arturbosch.detekt") version detektVersion
    }
}

rootProject.name = "simple-rate-retriever"