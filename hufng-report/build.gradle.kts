// Consumes DEVICE_TELEMETRY, writes to ClickHouse, checks alert rules, publishes RESOURCE_ALERT
plugins {
    id("org.springframework.boot")
}

dependencies {
    implementation(project(":hufng-common"))

    implementation("org.springframework.boot:spring-boot-starter-web")
    // Consumes DEVICE_TELEMETRY, produces RESOURCE_ALERT
    implementation("org.springframework.kafka:spring-kafka")
    // JdbcTemplate only, ClickHouse has no JPA/Hibernate dialect worth using here
    implementation("org.springframework.boot:spring-boot-starter-jdbc")
    implementation("com.clickhouse:clickhouse-jdbc")
    // Registers with Eureka so hufng-gateway can route to this service by id (lb://hufng-report)
    implementation("org.springframework.cloud:spring-cloud-starter-netflix-eureka-client")
    // GET /actuator/health for liveness checks
    implementation("org.springframework.boot:spring-boot-starter-actuator")

    testImplementation("org.springframework.boot:spring-boot-starter-test")
    testImplementation("org.springframework.kafka:spring-kafka-test")
}

// Only the executable bootJar is needed (Dockerfile copies build/libs/*.jar)
tasks.named<Jar>("jar") {
    enabled = false
}
