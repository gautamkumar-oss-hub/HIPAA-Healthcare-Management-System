FROM maven:3.9-eclipse-temurin-21 AS build
WORKDIR /workspace
COPY pom.xml .
COPY services services
ARG SERVICE
RUN mvn -B -pl "services/${SERVICE}" -am package -DskipTests

FROM eclipse-temurin:21-jre
WORKDIR /app
ARG SERVICE
COPY --from=build "/workspace/services/${SERVICE}/target/${SERVICE}-1.0.0-SNAPSHOT.jar" app.jar
EXPOSE 8080
ENTRYPOINT ["java", "-XX:MaxRAMPercentage=75.0", "-jar", "/app/app.jar"]

