FROM maven:3.9-eclipse-temurin-21 AS build
WORKDIR /app
COPY pom.xml .
COPY src ./src
RUN mvn -B -DskipTests package

FROM eclipse-temurin:21-jre
WORKDIR /app
RUN groupadd --system spice && useradd --system --gid spice spice
COPY --from=build /app/target/*.jar /app/app.jar
USER spice
EXPOSE 8080
ENTRYPOINT ["java", "-jar", "/app/app.jar"]
