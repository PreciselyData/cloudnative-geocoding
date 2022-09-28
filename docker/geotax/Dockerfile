# we are extending everything from openjdk11:alpine-jre image ...
FROM adoptopenjdk/openjdk11:alpine-jre

COPY gtx /usr/local/gtx

RUN apk update \
    && apk add build-base \
    && apk add g++ \
    && apk add mpc1-dev \
    && apk add gmp-dev \
    && apk add mpfr-dev \
    && apk add libc-dev

RUN cd /tmp \
    && mkdir /usr/local/rclone \
    && wget -q https://downloads.rclone.org/rclone-current-linux-amd64.zip \
    && unzip /tmp/rclone-current-linux-amd64.zip \
    && cp /tmp/rclone-*-linux-amd64/rclone /usr/bin \
    && rm -r /tmp/rclone*

ENV LD_LIBRARY_PATH=/usr/local/gtx/taxing-resources/bin/linux64
ENV JAVA_TOOL_OPTIONS='-Xmx6g -Xms2g'

WORKDIR /usr/local/gtx
EXPOSE 8080
CMD java -XX:+UseCompressedOops -XX:+UseG1GC -XX:InitiatingHeapOccupancyPercent=30 -XX:MaxGCPauseMillis=10000 -XX:MaxRAMPercentage=70.0 -jar geotax-rest-war-1.0-SNAPSHOT.war