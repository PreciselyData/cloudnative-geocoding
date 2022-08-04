# we are extending everything from openjdk11:alpine-jre image ...
FROM adoptopenjdk/openjdk11:alpine-jre

COPY gtx /usr/local/gtx

RUN cd /tmp \
    && mkdir /usr/local/rclone \
    && wget -q https://downloads.rclone.org/rclone-current-linux-amd64.zip \
    && unzip /tmp/rclone-current-linux-amd64.zip \
    && cp /tmp/rclone-*-linux-amd64/rclone /usr/bin \
    && rm -r /tmp/rclone*

WORKDIR /usr/local/gtx
EXPOSE 8080
CMD java -XX:+UseCompressedOops -XX:+UseG1GC -XX:InitiatingHeapOccupancyPercent=30 -XX:MaxGCPauseMillis=10000 -XX:MaxRAMPercentage=70.0 -jar geotax-rest-war-1.0-SNAPSHOT.war