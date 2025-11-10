#FROM maven:3.9.9-eclipse-temurin-21 AS builder
#
#WORKDIR /app
#
## Fix SSL issues: refresh certs, set TLSv1.2+, install curl for debugging
#RUN apt-get update && apt-get install -y ca-certificates curl openssl && update-ca-certificates
#ENV MAVEN_OPTS="-Dhttps.protocols=TLSv1.2,TLSv1.3"
#
## Copy pom.xml first to cache dependencies
#COPY pom.xml .
#
## Custom Maven settings to avoid bad_record_mac and retry downloads
#RUN mkdir -p /root/.m2 && echo '\
#<settings xmlns="http://maven.apache.org/SETTINGS/1.0.0" \
#  xmlns:xsi="http://www.w3.org/2001/XMLSchema-instance" \
#  xsi:schemaLocation="http://maven.apache.org/SETTINGS/1.0.0 \
#  https://maven.apache.org/xsd/settings-1.0.0.xsd"> \
#  <mirrors> \
#    <mirror> \
#      <id>central</id> \
#      <mirrorOf>*</mirrorOf> \
#      <url>https://repo.maven.apache.org/maven2/</url> \
#    </mirror> \
#  </mirrors> \
#  <proxies/> \
#  <servers/> \
#  <profiles> \
#    <profile> \
#      <id>reliable-downloads</id> \
#      <properties> \
#        <maven.wagon.http.retryHandler.count>5</maven.wagon.http.retryHandler.count> \
#        <maven.wagon.http.retryHandler.requestSentEnabled>true</maven.wagon.http.retryHandler.requestSentEnabled> \
#        <maven.wagon.httpconnectionManager.ttlSeconds>60</maven.wagon.httpconnectionManager.ttlSeconds> \
#        <maven.wagon.http.ssl.insecure>false</maven.wagon.http.ssl.insecure> \
#        <maven.wagon.http.ssl.allowall>false</maven.wagon.http.ssl.allowall> \
#      </properties> \
#    </profile> \
#  </profiles> \
#  <activeProfiles> \
#    <activeProfile>reliable-downloads</activeProfile> \
#  </activeProfiles> \
#</settings>' > /root/.m2/settings.xml
#
## Verify connectivity to Maven Central
#RUN curl -I https://repo.maven.apache.org/maven2/
#
## Download dependencies (with retries)
#RUN mvn -B dependency:go-offline --fail-never -Dhttps.protocols=TLSv1.2,TLSv1.3
#
## Copy source code
#COPY src ./src
#
## Build app
#RUN mvn -B clean package -Dmaven.test.skip=true -Dhttps.protocols=TLSv1.2,TLSv1.3
#
## Runtime image
#FROM eclipse-temurin:21-jdk AS runner
#WORKDIR /app
#COPY --from=builder /app/target/api-gateway-0.0.1-SNAPSHOT.jar ./app.jar
#EXPOSE 4004
#ENTRYPOINT ["java", "-jar", "app.jar"]



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
COPY --from=builder /app/target/api-gateway-0.0.1-SNAPSHOT.jar ./app.jar

EXPOSE 4004

ENTRYPOINT ["java", "-jar", "app.jar"]
