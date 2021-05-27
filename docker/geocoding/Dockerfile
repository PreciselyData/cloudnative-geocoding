# we are extending everything from tomcat:8.0 image ...
FROM tomcat:8.5-alpine as build

COPY ggs /usr/local/ggs	
COPY deploy-ggs.sh /usr/local/ggs/

RUN dos2unix /usr/local/ggs/deploy-ggs.sh \
    && chmod 755 /usr/local/ggs/deploy-ggs.sh \
    && chmod 755 /usr/local/ggs/cli/cli.sh \
    && /usr/local/ggs/cli/cli.sh deploy --c /usr/local/ggs/resources --d $CATALINA_HOME/webapps/geocode --l $CATALINA_HOME/logs/ --m WAR_EXTRACTED \
	&& rm -rf /usr/local/ggs/webapp 

RUN cd /tmp \
    && mkdir /usr/local/rclone \
    && wget -q https://downloads.rclone.org/rclone-current-linux-amd64.zip \
    && unzip /tmp/rclone-current-linux-amd64.zip \
    && mv /tmp/rclone-*-linux-amd64/rclone /usr/local/rclone \
    && rm -r /tmp/rclone*	

FROM tomcat:8.5-alpine
MAINTAINER Precisely

RUN apk --no-cache add bc

RUN apk --no-cache add ca-certificates fuse tzdata \
    && echo "user_allow_other" >> /etc/fuse.conf \
    && rm -rf /var/cache/apk/*
	
## Copy rclone cli from build stage
COPY --from=build /usr/local/rclone /usr/bin
	
## Copy configured ggs libraries from build stage
COPY --from=build /usr/local/ggs /usr/local/ggs
COPY --from=build $CATALINA_HOME/webapps/geocode $CATALINA_HOME/webapps/geocode
	
CMD ["/usr/local/ggs/deploy-ggs.sh"]
