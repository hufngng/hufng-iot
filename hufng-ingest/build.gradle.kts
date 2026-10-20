// MQTT bridge: subscribes device telemetry, authenticates the device, produces to Kafka DEVICE_TELEMETRY
plugins {
    id("org.springframework.boot")
}

dependencies {
    implementation(project(":hufng-common"))

    // MQTT inbound (Paho client underneath)
    implementation("org.springframework.integration:spring-integration-mqtt")
    // Produces DEVICE_TELEMETRY
    implementation("org.springframework.kafka:spring-kafka")
    // Device registry (device table)
    implementation("org.springframework.boot:spring-boot-starter-data-jpa")
    runtimeOnly("com.mysql:mysql-connector-j")
    // Only need BCryptPasswordEncoder to verify device secrets, no filter chain here
    implementation("org.springframework.security:spring-security-crypto")
    // Register endpoint
    implementation("org.springframework.boot:spring-boot-starter-web")
    // Registers with Eureka so hufng-gateway (if present) can route to this service by id
    implementation("org.springframework.cloud:spring-cloud-starter-netflix-eureka-client")
    // GET /actuator/health for liveness checks
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
