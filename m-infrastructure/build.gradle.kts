import org.jetbrains.kotlin.gradle.tasks.KotlinCompile
import java.util.*

plugins {
    id("org.springframework.boot") version "3.0.6"
    id("io.spring.dependency-management") version "1.1.0"
    id("org.liquibase.gradle") version "2.0.4"
    kotlin("jvm") version "1.7.22"
    kotlin("plugin.spring") version "1.7.22"
    kotlin("plugin.jpa") version "1.7.22"
}

group = "com.example"
version = "0.0.1-SNAPSHOT"
java.sourceCompatibility = JavaVersion.VERSION_17

configurations {
    compileOnly {
        extendsFrom(configurations.annotationProcessor.get())
    }
}

repositories {
    mavenCentral()
}

dependencies {
    implementation("org.springframework.boot:spring-boot-starter-data-jpa")
    implementation("org.springframework.boot:spring-boot-starter-web")
    implementation("org.springframework.boot:spring-boot-starter-validation")
    implementation("org.springdoc:springdoc-openapi-starter-webmvc-ui:2.1.0")
    implementation("org.jetbrains.kotlin:kotlin-reflect")
    implementation("org.liquibase:liquibase-core")
    implementation(project(mapOf("path" to ":m-application-services")))
    implementation(project(mapOf("path" to ":m-domain-models")))
    developmentOnly("org.springframework.boot:spring-boot-devtools")
    runtimeOnly("org.postgresql:postgresql")
    annotationProcessor("org.springframework.boot:spring-boot-configuration-processor")
    testImplementation("org.springframework.boot:spring-boot-starter-test")
    liquibaseRuntime( "org.liquibase:liquibase-core:4.16.1")
    liquibaseRuntime("org.liquibase:liquibase-groovy-dsl:3.0.2")
    liquibaseRuntime("info.picocli:picocli:4.6.1")
    liquibaseRuntime("org.postgresql:postgresql:42.2.27")
    liquibaseRuntime("org.liquibase.ext:liquibase-hibernate6:4.21.1")
    liquibaseRuntime("org.springframework.boot:spring-boot-starter-data-jpa")
    liquibaseRuntime(sourceSets.getByName("main").output)
    liquibaseRuntime(sourceSets.getByName("main").compileClasspath)
    liquibaseRuntime(sourceSets.getByName("main").runtimeClasspath)

}

val cfg = project.file("src/main/resources/application.yaml")

val projectProperties = Properties().apply {
    load(cfg.inputStream())
}

tasks.diffChangeLog {
    dependsOn("compileKotlin")
}

configurations {
    compileOnly {
        extendsFrom(configurations.annotationProcessor.get())
    }
    liquibase {
        activities.register("main") {
            this.arguments = mapOf(
                "changeLogFile" to (projectProperties["spring.liquibase.change-log"] as String).replace("classpath:", "m-infrastructure/src/main/resources/"),
                "url" to projectProperties["spring.datasource.url"],
                "referenceUrl" to "hibernate:spring:com.esgi.fpr?dialect=org.hibernate.dialect.PostgreSQLDialect&hibernate.physical_naming_strategy=org.hibernate.boot.model.naming.CamelCaseToUnderscoresNamingStrategy&hibernate.implicit_naming_strategy=org.springframework.boot.orm.jpa.hibernate.SpringImplicitNamingStrategy",
                "driver" to projectProperties["spring.datasource.driver-class-name"],
                "referenceDriver" to "liquibase.ext.hibernate.database.connection.HibernateDriver",
                "username" to projectProperties["spring.datasource.username"],
                "password" to projectProperties["spring.datasource.password"],
            )
        }
        runList = "main"
    }
}



tasks.withType<KotlinCompile> {
    kotlinOptions {
        freeCompilerArgs = listOf("-Xjsr305=strict")
        jvmTarget = "17"
    }
}

tasks.withType<Test> {
    useJUnitPlatform()
}