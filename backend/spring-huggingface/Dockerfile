FROM maven:3.9.9-eclipse-temurin-17 AS build

WORKDIR /workspace

# Copy only what we need for dependency caching
COPY backend/spring-huggingface/pom.xml ./pom.xml
COPY backend/spring-huggingface/src ./src

RUN mvn -B -DskipTests=false test package

FROM eclipse-temurin:17-jre

WORKDIR /app

RUN apt-get update \
  && apt-get install -y --no-install-recommends curl \
  && rm -rf /var/lib/apt/lists/*

COPY --from=build /workspace/target/*.jar /app/app.jar

EXPOSE 8080

ENV SERVER_PORT=8080

ENTRYPOINT ["java", "-jar", "/app/app.jar"]
