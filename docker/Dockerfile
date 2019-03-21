FROM openjdk:alpine
MAINTAINER Phil Schatzmann <pschatzmann@gmail.com>
RUN apk add --update tar rsync docker rdiff-backup p7zip
RUN mkdir -p /opt/docker-cron
COPY *.jar /opt/docker-cron/
ENV xmx 600m 
WORKDIR /opt/docker-cron
CMD java -Djsse.enableSNIExtension=false -Xmx$xmx -server -jar /opt/docker-cron/docker-cron-0.0.1-SNAPSHOT-jar-with-dependencies.jar 

