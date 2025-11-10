# === Build stage ===
FROM maven:3.9.9-eclipse-temurin-21 AS builder

WORKDIR /app

# Copy only the pom.xml first to cache dependencies
COPY pom.xml .

# Download dependencies
RUN mvn dependency:go-offline -B

# Copy the rest of the project files
COPY src ./src

# Build the application (skip tests)
RUN mvn clean package -DskipTests

# === Run stage ===
FROM eclipse-temurin:21-jdk AS runner

WORKDIR /app

# Copy the JAR file from the build stage
COPY --from=builder /app/target/patient-service-0.0.1-SNAPSHOT.jar ./app.jar

EXPOSE 4000

ENTRYPOINT ["java", "-jar", "app.jar"]
