FROM openjdk:16-jdk-slim

COPY build/libs/CozyDiscord-*-all.jar /usr/local/lib/CozyDiscord.jar

RUN mkdir /bot
RUN mkdir /data
WORKDIR /bot

ENTRYPOINT ["java", "-Xms2G", "-Xmx2G", "-Dkotlinx.coroutines.debug=true", "-jar", "/usr/local/lib/CozyDiscord.jar"]
