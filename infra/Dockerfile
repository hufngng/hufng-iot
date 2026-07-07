# Eureka Server — service registry so hufng-gateway can route to hufng-auth
# (and future services) by service id (lb://hufng-auth) instead of a static
# host:port. Not a Maven module in this reactor — this wraps the community
# Steeltoe Eureka Server image, which speaks the standard Spring Cloud Netflix
# Eureka protocol, so hufng-auth/hufng-gateway register against it exactly as
# they would against a hand-built Eureka server.
FROM steeltoeoss/eureka-server:latest
EXPOSE 8761
