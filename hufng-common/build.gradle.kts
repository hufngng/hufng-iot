// Shared utilities and common code for hufng-iot modules — plain library jar, no bootJar
plugins {
    `java-library`
}

dependencies {
    // `api` (not `implementation`) mirrors Maven compile scope: these leak transitively
    // to every service that depends on hufng-common.
    api("org.springframework.boot:spring-boot-starter")
    api("org.springframework.boot:spring-boot-starter-web")
    api("org.springframework.boot:spring-boot-starter-aop")
    // Redis via lettuce (Spring Boot default): non-blocking, thread-safe,
    // supports sync/async (CompletableFuture) and reactive (Mono/Flux), depends on Netty.
    // jedis alternative is blocking and not thread-safe (1 connection per thread).
    api("org.springframework.boot:spring-boot-starter-data-redis")

    api("com.github.pagehelper:pagehelper:6.1.0")
    api("org.springdoc:springdoc-openapi-starter-webmvc-ui:2.6.0")
    api("net.logstash.logback:logstash-logback-encoder:7.4")

    testImplementation("org.springframework.boot:spring-boot-starter-test")
}
