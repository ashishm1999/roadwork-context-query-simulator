FROM maven:3.9-eclipse-temurin-11 AS build
WORKDIR /build
COPY pom.xml .
COPY src ./src
RUN mvn -q package -DskipTests

FROM eclipse-temurin:11-jre
WORKDIR /app
COPY --from=build /build/target/roadwork-context-query-simulator-1.0.0.jar app.jar
COPY sample /data
ENV QUERY_LOAD=/data/diurnal-queries.json
ENV COAAS_URL=http://coaas:8080
ENTRYPOINT ["sh", "-c", "java -jar /app/app.jar --queries $QUERY_LOAD --coaas $COAAS_URL"]
