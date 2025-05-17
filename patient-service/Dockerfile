FROM maven:3.9.9-eclipse-temurin-21 AS builder

WORKDIR /app

# Copy only the pom.xml to leverage Docker cache for dependencies
COPY pom.xml .

## Explicitly set DNS servers (try Google's public DNS)
#RUN echo "nameserver 8.8.8.8" > /etc/resolv.conf
#RUN echo "nameserver 8.8.4.4" >> /etc/resolv.conf

# Download dependencies
RUN mvn dependency:go-offline -B

# Copy the rest of the application source code
COPY src ./src

# Package the application
RUN mvn clean package -DskipTests

FROM openjdk:21-jdk AS runner

WORKDIR /app

# Copy the packaged JAR from the builder stage
COPY --from=builder /app/target/patient-service-0.0.1-SNAPSHOT.jar ./app.jar

EXPOSE 4000

ENTRYPOINT ["java", "-jar", "app.jar"]