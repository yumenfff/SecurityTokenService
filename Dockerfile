FROM maven:3.9-eclipse-temurin-17 AS builder

WORKDIR /build
COPY pom.xml .
COPY src ./src

RUN mvn clean package -DskipTests

FROM eclipse-temurin:17-jre

WORKDIR /app

# shade output = app.jar
COPY --from=builder /build/target/app.jar app.jar

EXPOSE 8080

CMD ["java", "-jar", "app.jar"]
