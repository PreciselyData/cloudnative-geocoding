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
		python \
		py-pip \
		parallel \
	&& pip install --upgrade pip pyasn1-modules \	
    && pip install awscli 

## Install gsutil CLI	
RUN apk add --update \
       py-cffi \
       py-cryptography \
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

## Install Azure's azure-storage-blob python API to generate Azure SAS token	
RUN apk add --update \
       libc6-compat \
	   ca-certificates \
    && pip install azure-storage-blob 

## Copy azcopy CLI from build stage	
COPY --from=build /usr/local/azure/azcopy /usr/local/bin/azcopy	

## Copy configured ggs libraries from build stage
COPY --from=build /usr/local/ggs /usr/local/ggs
COPY --from=build $CATALINA_HOME/webapps/geocode $CATALINA_HOME/webapps/geocode
	
CMD ["/usr/local/ggs/deploy-ggs.sh"]
