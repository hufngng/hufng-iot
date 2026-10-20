// Notification service — idempotent consumer (processed_event) routes RESOURCE_ALERT to Telegram/Discord
plugins {
    id("org.springframework.boot")
}

dependencies {
    implementation(project(":hufng-common"))

    implementation("org.springframework.boot:spring-boot-starter-web")
    // Consumes RESOURCE_ALERT topic, routes each message to a notification provider
    implementation("org.springframework.kafka:spring-kafka")
    // processed_event table for write-time exact idempotency (INSERT-first, PK violation = duplicate)
    implementation("org.springframework.boot:spring-boot-starter-data-jpa")
    runtimeOnly("com.mysql:mysql-connector-j")
    // Registers with Eureka so hufng-gateway can route to this service by id (lb://hufng-notification)
    implementation("org.springframework.cloud:spring-cloud-starter-netflix-eureka-client")
    // GET /actuator/health for liveness checks (Eureka health check, load balancers, ...)
    implementation("org.springframework.boot:spring-boot-starter-actuator")

    testImplementation("org.springframework.boot:spring-boot-starter-test")
    testImplementation("org.springframework.kafka:spring-kafka-test")
    // In-memory DB replacing MySQL in tests
    testRuntimeOnly("com.h2database:h2")
}

// Only the executable bootJar is needed (Dockerfile copies build/libs/*.jar)
tasks.named<Jar>("jar") {
    enabled = false
}
