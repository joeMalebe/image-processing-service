# Stage 1: Build the application with Gradle
FROM gradle:9.3.0-jdk21 AS build
WORKDIR /home/gradle/src
COPY --chown=gradle:gradle . /home/gradle/src
# Use the Gradle wrapper to ensure a consistent Gradle version
RUN gradle buildFatJar --no-daemon

# Stage 2: Create the final runtime image
# Use a lightweight JDK image, e.g., Eclipse Temurin or Amazon Corretto
FROM eclipse-temurin:21-jre-jammy AS runtime
EXPOSE 8080
WORKDIR /app

# Copy the built JAR file from the 'build' stage
# Adjust 'app.jar' to match the actual name of your generated JAR file (e.g., in build/libs/)
COPY --from=build /home/gradle/src/build/libs/image-processing-all.jar /app/app.jar

# Command to run the application
ENTRYPOINT ["java","-jar","/app/app.jar"]