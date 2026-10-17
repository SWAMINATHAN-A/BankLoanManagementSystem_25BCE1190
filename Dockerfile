# Multi-stage Dockerfile for Bank Loan Management System
# Base build image: OpenJDK 21 on Alpine Linux
FROM eclipse-temurin:21-jdk-alpine AS builder

WORKDIR /app

# Copy source, libraries, and web assets
COPY src/ ./src/
COPY lib/ ./lib/
COPY web/ ./web/

# Compile Java application bytecode into bin/
RUN mkdir -p bin && \
    javac -cp "lib/*" -d bin $(find src -name "*.java")

# Runtime image: Minimal OpenJDK 21 JRE
FROM eclipse-temurin:21-jre-alpine

WORKDIR /app

# Copy compiled classes, libraries, and static web directory
COPY --from=builder /app/bin ./bin
COPY --from=builder /app/lib ./lib
COPY --from=builder /app/web ./web

# Expose default HTTP port
EXPOSE 8085

# Environment variable defaults (overrideable by cloud host)
ENV PORT=8085

# Start Java HTTP server
CMD ["java", "-cp", "bin:lib/*", "com.bankloan.Main"]
