// IoT ingestion pipeline: device -> MQTT -> Kafka -> ClickHouse + rule engine -> alert
plugins {
    java
    jacoco
    id("org.springframework.boot") version "3.3.4" apply false
    id("io.spring.dependency-management") version "1.1.6" apply false
}

val springBootVersion: String by project
val springCloudVersion: String by project
val clickhouseJdbcVersion: String by project

allprojects {
    group = "com.hufng.iot"
    version = "0.1.0-SNAPSHOT"
}

// Aggregated coverage across all modules: ./gradlew jacocoRootReport
// -> build/reports/jacoco/jacocoRootReport/html/index.html (+ jacocoRootReport.xml).
// Hand-rolled instead of the `jacoco-report-aggregation` plugin: that plugin resolves every
// module's dependency graph from the root project, which has no Spring BOM, so versionless
// starters fail to resolve.
repositories {
    mavenCentral()
}

val jacocoRootReport by tasks.registering(JacocoReport::class) {
    group = "verification"
    description = "Generates an aggregated JaCoCo coverage report for all subprojects."

    subprojects.forEach { sub ->
        dependsOn(sub.tasks.named("test"))
        val main = sub.the<SourceSetContainer>()["main"]
        sourceDirectories.from(main.allSource.srcDirs)
        classDirectories.from(main.output)
        executionData.from(sub.layout.buildDirectory.file("jacoco/test.exec"))
    }

    reports {
        xml.required = true
        html.required = true
    }
}

subprojects {
    apply(plugin = "java")
    apply(plugin = "io.spring.dependency-management")
    apply(plugin = "jacoco")

    repositories {
        mavenCentral()
    }

    java {
        toolchain {
            languageVersion = JavaLanguageVersion.of(21)
        }
    }

    // Replaces the spring-boot-starter-parent + <dependencyManagement> of the old root pom.xml
    the<io.spring.gradle.dependencymanagement.dsl.DependencyManagementExtension>().apply {
        imports {
            mavenBom("org.springframework.boot:spring-boot-dependencies:$springBootVersion")
            // Shared by hufng-ingest/hufng-report/hufng-notification (eureka client)
            mavenBom("org.springframework.cloud:spring-cloud-dependencies:$springCloudVersion")
        }
        dependencies {
            // hufng-report: JDBC driver for ClickHouse (telemetry storage)
            dependency("com.clickhouse:clickhouse-jdbc:$clickhouseJdbcVersion")
        }
    }

    tasks.withType<JavaCompile>().configureEach {
        options.encoding = "UTF-8"
        // Same as spring-boot-starter-parent: keep parameter names for @PathVariable/@RequestParam etc.
        options.compilerArgs.add("-parameters")
    }

    tasks.withType<Test>().configureEach {
        useJUnitPlatform()
        finalizedBy(tasks.named("jacocoTestReport"))
    }

    // Coverage: build/reports/jacoco/test/html/index.html (+ jacocoTestReport.xml for CI/Sonar)
    tasks.named<JacocoReport>("jacocoTestReport") {
        dependsOn(tasks.named("test"))
        reports {
            xml.required = true
            html.required = true
        }
    }
}
