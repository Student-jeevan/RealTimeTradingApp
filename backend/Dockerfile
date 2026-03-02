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

# Optional credentials directory (local only)
COPY credentials /app/credentials

# Expose backend port inside container
EXPOSE 8081

# Run the Spring Boot application
# Uses PORT env if set, otherwise defaults to 8081
ENTRYPOINT ["sh", "-c", "java -jar app.jar --server.address=0.0.0.0 --server.port=${PORT:-8081}"]
