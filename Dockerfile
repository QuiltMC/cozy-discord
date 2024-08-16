FROM openjdk:17-jdk-slim

COPY build/libs/CozyDiscord-*-all.jar /usr/local/lib/CozyDiscord.jar

RUN mkdir /bot
RUN mkdir /bot/data
RUN mkdir /bot/plugins

WORKDIR /bot

VOLUME /bot/data
VOLUME /bot/plugins

ENTRYPOINT ["java", "-Xms2G", "-Xmx2G", "-jar", "/usr/local/lib/CozyDiscord.jar"]
