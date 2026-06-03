FROM ghcr.io/adoptium/temurin:8-jre-jammy

RUN apt-get update \
    && apt-get install -y --no-install-recommends \
        ca-certificates \
        ffmpeg \
        tzdata \
    && rm -rf /var/lib/apt/lists/*

WORKDIR /app

RUN groupadd --system app \
    && useradd --system --gid app --home-dir /app app \
    && mkdir -p /app/logs /tmp/video-processing /tmp/video-nfs/dev \
    && chown -R app:app /app /tmp/video-processing /tmp/video-nfs

COPY target/*.jar /app/app.jar

USER app

EXPOSE 8080 9091 9092

ENV JAVA_OPTS="" \
    SPRING_PROFILES_ACTIVE=prod

ENTRYPOINT ["sh", "-c", "java $JAVA_OPTS -jar /app/app.jar"]
