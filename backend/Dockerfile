# syntax=docker/dockerfile:1.7

FROM eclipse-temurin:25-jdk AS build
WORKDIR /workspace

COPY mvnw ./
COPY .mvn .mvn
COPY pom.xml ./

RUN chmod +x mvnw \
 && ./mvnw -B -ntp -q dependency:go-offline

COPY src src
RUN ./mvnw -B -ntp -q -DskipTests package \
 && cp target/*.jar app.jar

FROM eclipse-temurin:25-jre
WORKDIR /app

RUN apt-get update \
 && apt-get install -y --no-install-recommends curl \
 && rm -rf /var/lib/apt/lists/* \
 && groupadd --system --gid 1500 votacao \
 && useradd --system --uid 1500 --gid votacao --home /app votacao

COPY --from=build --chown=votacao:votacao /workspace/app.jar /app/app.jar

USER votacao
EXPOSE 8080

ENV JAVA_OPTS=""
ENTRYPOINT ["sh", "-c", "exec java $JAVA_OPTS -jar /app/app.jar"]
