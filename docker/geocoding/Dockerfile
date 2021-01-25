# we are extending everything from tomcat:8.0 image ...
FROM tomcat:8.5-alpine as deploy-ggs

COPY ggs /usr/local/ggs	
COPY deploy-ggs.sh /usr/local/ggs/

RUN dos2unix /usr/local/ggs/deploy-ggs.sh \
    && chmod 755 /usr/local/ggs/deploy-ggs.sh \
    && chmod 755 /usr/local/ggs/cli/cli.sh \
    && /usr/local/ggs/cli/cli.sh deploy --c /usr/local/ggs/resources --d $CATALINA_HOME/webapps/geocode --l $CATALINA_HOME/logs/ --m WAR_EXTRACTED \
	&& rm -rf /usr/local/ggs/webapp 

FROM tomcat:8.5-alpine
MAINTAINER Precisely

## Install AWS CLI and dos2unix
RUN apk update -q \
    && apk add --update \
   	    dos2unix \
		python \
		py-pip \
		parallel \
    && pip install --upgrade pyasn1-modules awscli 

## Install gsutil CLI	
RUN apk add --update \
       py-cffi \
       py-cryptography \
    && pip install --upgrade pip \
    && apk add --virtual build-deps \
       gcc \
       libffi-dev \
       python-dev \
       linux-headers \
       musl-dev \
       openssl-dev \
    && pip install gsutil \
    && apk del build-deps \
    && apk add --upgrade bc \
    && rm -rf /var/cache/apk/* 

## Copy configured ggs libraries from deploy-ggs stage
COPY --from=deploy-ggs /usr/local/ggs /usr/local/ggs
COPY --from=deploy-ggs $CATALINA_HOME/webapps/geocode $CATALINA_HOME/webapps/geocode
	
CMD ["/usr/local/ggs/deploy-ggs.sh"]
