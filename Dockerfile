FROM gradle:8.10-jdk17-jammy AS builder

WORKDIR /home/gradle/project

COPY build.gradle settings.gradle ./
RUN --mount=type=cache,target=/home/gradle/.gradle,uid=1000,gid=1000 gradle dependencies --no-daemon

COPY src src
RUN --mount=type=cache,target=/home/gradle/.gradle,uid=1000,gid=1000 gradle bootJar --no-daemon -x test

# Stage 2
FROM eclipse-temurin:17-jre-jammy AS extractor

WORKDIR /extract

COPY --from=builder /home/gradle/project/build/libs/app.jar app.jar
RUN java -Djarmode=tools -jar app.jar extract --destination extracted


# Stage 3
FROM eclipse-temurin:17-jre-jammy

RUN apt-get update \
    && apt-get install -y --no-install-recommends curl \
    && rm -rf /var/lib/apt/lists/*

WORKDIR /app

RUN groupadd --system appgroup \
    && useradd --system --gid appgroup --home-dir /app --shell /usr/sbin/nologin appuser
USER appuser

COPY --from=extractor --chown=appuser:appgroup /extract/extracted/lib lib/
COPY --from=extractor --chown=appuser:appgroup /extract/extracted/app.jar app.jar

ENV JAVA_OPTS="-XX:+UseG1GC -XX:MaxRAMPercentage=75.0 -Djava.security.egd=file:/dev/./urandom"

EXPOSE 8080

HEALTHCHECK --interval=15s --timeout=5s --retries=3 --start-period=60s \
  CMD curl -fsS http://localhost:8080/actuator/health | grep -q '"status":"UP"' || exit 1

ENTRYPOINT ["sh", "-c", "exec java $JAVA_OPTS -jar app.jar"]
