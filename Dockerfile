<<<<<<< HEAD
=======
# Stage 1: Build
>>>>>>> 7a7c6ca0298ce2373f92a95118355849d2412fdc
FROM gradle:8.5-jdk17-alpine AS build
WORKDIR /app
COPY . .
RUN gradle bootJar --no-daemon -x test

<<<<<<< HEAD
=======
# Stage 2: Run
>>>>>>> 7a7c6ca0298ce2373f92a95118355849d2412fdc
FROM eclipse-temurin:17-jre-alpine
WORKDIR /app
COPY --from=build /app/build/libs/*.jar app.jar
EXPOSE 8080
ENTRYPOINT ["java", "-Xmx512m", "-jar", "app.jar"]
