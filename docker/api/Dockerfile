FROM eclipse-temurin:21-jdk AS build
WORKDIR /app

# Copy gradle configuration
COPY gradlew .
COPY gradle gradle
COPY build.gradle .
COPY settings.gradle .

# Make gradlew executable
RUN chmod +x ./gradlew

# Copy source code
COPY src src

# Build the JAR with optimizations
RUN ./gradlew shadowJar --no-daemon -Dorg.gradle.parallel=true -Dorg.gradle.caching=true

# Create the runtime container
FROM eclipse-temurin:21-jre
WORKDIR /app

# Copy the JAR from the build stage
COPY --from=build /app/build/libs/rinha-backend.jar /app/app.jar

# Set the command to run the application with JVM tuning for maximum performance
ENTRYPOINT ["java", \
  "--enable-preview", \
  "-XX:+UseZGC", \
  "-XX:ZCollectionInterval=600", \
  "-XX:+DisableExplicitGC", \
  "-XX:+AlwaysPreTouch", \
  "-XX:+UnlockExperimentalVMOptions", \
  "-XX:InitiatingHeapOccupancyPercent=75", \
  "-XX:+UseStringDeduplication", \
  "-Xms128m", \
  "-Xmx150m", \
  "-jar", "/app/app.jar"]