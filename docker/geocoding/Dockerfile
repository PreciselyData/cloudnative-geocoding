# we are extending everything from tomcat:8.0 image ...
FROM tomcat:8.5-alpine as build

COPY ggs /usr/local/ggs	
COPY deploy-ggs.sh /usr/local/ggs/

RUN dos2unix /usr/local/ggs/deploy-ggs.sh \
    && chmod 755 /usr/local/ggs/deploy-ggs.sh \
    && chmod 755 /usr/local/ggs/cli/cli.sh \
    && /usr/local/ggs/cli/cli.sh deploy --c /usr/local/ggs/resources --d $CATALINA_HOME/webapps/geocode --l $CATALINA_HOME/logs/ --m WAR_EXTRACTED \
	&& rm -rf /usr/local/ggs/webapp 

## Install azcopy CLI	
RUN mkdir /usr/local/azure
RUN apk add --no-cache tar wget \
    && wget https://aka.ms/downloadazcopy-v10-linux -O /tmp/azcopy.tgz \
    && tar -xzf /tmp/azcopy.tgz -C /usr/local/azure --strip-components 1
	
FROM tomcat:8.5-alpine
MAINTAINER Precisely

## Install AWS CLI
RUN apk update -q \
    && apk add --update \
		python3 \
		py3-pip \
		parallel \
	&& pip3 install --upgrade pip pyasn1-modules \	
    && pip3 install awscli 

## Install gsutil CLI	
RUN apk add --update \
       libc6-compat \
	   ca-certificates \
       py3-cffi \
       py3-cryptography \
    && apk add --virtual build-deps \
       gcc \
       libffi-dev \
       python3-dev \
       linux-headers \
       musl-dev \
       openssl-dev \
    && pip3 install gsutil \
    && apk del build-deps \
    && apk add --upgrade bc \
    && rm -rf /var/cache/apk/* 

## Install Azure's azure-storage-blob python API to generate Azure SAS token	
RUN pip3 install azure-storage-blob
	
## Copy configured ggs libraries and azcopy cli from build stage
COPY --from=build /usr/local/ggs /usr/local/ggs
COPY --from=build $CATALINA_HOME/webapps/geocode $CATALINA_HOME/webapps/geocode
COPY --from=build /usr/local/azure/azcopy /usr/local/bin/azcopy
	
CMD ["/usr/local/ggs/deploy-ggs.sh"]
