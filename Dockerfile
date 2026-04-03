# ============================================================
# BankMind Backend — Spring Boot + Java 21
# Multi-stage build · Eclipse Temurin
# ============================================================

# ── Stage 1: Build ───────────────────────────────────────────
FROM eclipse-temurin:21-jdk-alpine AS build

WORKDIR /build

# Copiar archivos de Maven Wrapper primero para aprovechar la caché de capas
COPY pom.xml mvnw ./
COPY .mvn .mvn

# Dar permisos y descargar dependencias (esto no cambiará a menos que toques el pom.xml)
RUN chmod +x mvnw && ./mvnw dependency:go-offline -B

# Copiar el código fuente
COPY src src

# CONSEJO: Solo necesitas UNA instrucción de empaquetado. 
# "clean" asegura que no haya basura de builds previos.
RUN ./mvnw clean package -DskipTests -B

# ── Stage 2: Runtime ─────────────────────────────────────────
FROM eclipse-temurin:21-jre-alpine

LABEL maintainer="BankMind Team"
LABEL description="Sistema Bancario integrado con IA — Backend API"

# Instalar wget para que el HEALTHCHECK funcione en Alpine
RUN apk add --no-cache wget

WORKDIR /app

# Copiar el JAR. Usar el comodín * es útil si la versión cambia (ej: 0.0.1-SNAPSHOT)
COPY --from=build /build/target/*.jar app.jar

# Exponer el puerto
EXPOSE 8080

# Health check (Actuator debe estar en tu pom.xml para que esto no falle)
HEALTHCHECK --interval=30s --timeout=10s --retries=3 \
    CMD wget --spider -q http://localhost:8080/actuator/health || exit 1

# Comando de arranque optimizado para entornos de contenedores
ENTRYPOINT ["java", "-XX:+UseContainerSupport", "-XX:MaxRAMPercentage=75.0", "-jar", "app.jar", "--spring.profiles.active=docker"]