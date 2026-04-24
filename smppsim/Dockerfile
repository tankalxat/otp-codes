FROM maven:3.9-eclipse-temurin-8 AS build

RUN apt-get update && apt-get install -y --no-install-recommends git unzip \
    && rm -rf /var/lib/apt/lists/*

WORKDIR /workspace

RUN git clone --depth 1 https://github.com/MavoCz/smscsim.git .

RUN mvn -B -q clean package assembly:assembly \
    && unzip -q target/smscsim-*-dist.zip -d /dist \
    && mv /dist/$(ls /dist | head -1) /dist/smscsim

FROM eclipse-temurin:17-jre

WORKDIR /app
COPY --from=build /dist/smscsim/ /app/

EXPOSE 2775

CMD ["sh", "-c", "java -Xms32m -Xmx256m -jar /app/smscsim.jar -ll INFO -p 2775"]
