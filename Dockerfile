# ── Stage 1: Build del .jar con Maven ───────────────────────────────────────
FROM maven:3.9-eclipse-temurin-21 AS builder

WORKDIR /app

# Copiar el descriptor de dependencias primero para aprovechar la caché de capas
COPY pom.xml .
RUN mvn dependency:go-offline -B

# Copiar el código fuente y compilar el .jar ejecutable
COPY src ./src
RUN mvn package -DskipTests -B

# ── Stage 2: Runtime ligero con solo JRE ─────────────────────────────────────
FROM eclipse-temurin:21-jre-alpine

WORKDIR /app

# Copiar únicamente el .jar generado desde el stage anterior
COPY --from=builder /app/target/*.jar app.jar

# Exponer el puerto del Backend Spring Boot
EXPOSE 8080

# Arrancar la aplicación
ENTRYPOINT ["java", "-jar", "app.jar"]
