
# Stage 1: Build the Spring Boot application
FROM eclipse-temurin:25-jdk AS build

WORKDIR /app

# Copy the complete Maven project
COPY . .

# Make Maven wrapper executable
RUN chmod +x mvnw

# Build the application without running tests
RUN ./mvnw -DskipTests package

# Stage 2: Run the packaged application
FROM eclipse-temurin:25-jre

WORKDIR /app

# Copy the built application from Stage 1
COPY --from=build /app/target/*.jar app.jar

# Spring Boot application port
EXPOSE 8080

# Start the Spring Boot application
ENTRYPOINT ["java", "-jar", "app.jar"]
