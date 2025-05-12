# Use OpenJDK base image to build and run the Spring Boot app
FROM openjdk:20-jdk-slim AS builder

# Install Maven
RUN apt-get update && apt-get install -y maven

# Set the working directory
WORKDIR /app

# Copy pom.xml and install dependencies first (for caching)
COPY pom.xml .
RUN mvn dependency:go-offline

# Copy the entire project and build the app
COPY . .
RUN ./mvnw clean package -DskipTests

# Use a smaller OpenJDK runtime image to run the app
FROM openjdk:20-jdk-slim

# Set the working directory
WORKDIR /app

# Copy the JAR file from the builder stage
COPY --from=builder /app/target/budget-service-0.0.1-SNAPSHOT.jar app.jar

# Expose port
EXPOSE 8082

# Run the JAR file
ENTRYPOINT ["java", "-jar", "app.jar"]
