# Step 1: Build stage
FROM maven:3.9.9-eclipse-temurin-17 AS build
WORKDIR /app

# Copy pom.xml and download dependencies
COPY pom.xml .
RUN mvn dependency:go-offline

# Copy project source and build
COPY src ./src
RUN mvn clean package -DskipTests

# Step 2: Run stage (lightweight JDK)
FROM eclipse-temurin:17-jdk-jammy
WORKDIR /app

# Copy the specific jar file to avoid wildcard issues
COPY --from=build /app/target/TradingApp-0.0.1-SNAPSHOT.jar app.jar

# Copy credentials directory
COPY credentials /app/credentials

# Set environment variable for Google Credentials
ENV GOOGLE_APPLICATION_CREDENTIALS=/app/credentials/tradingchatbot-service-account.json

# Expose backend port
EXPOSE 8080

# Run the Spring Boot application
ENTRYPOINT ["java", "-jar", "app.jar"]
