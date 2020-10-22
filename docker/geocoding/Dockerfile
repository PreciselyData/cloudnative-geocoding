# we are extending everything from tomcat:8.0 image ...
FROM tomcat:8.5-alpine
MAINTAINER pb

COPY ggs /usr/local/ggs

RUN apk update -q
RUN apk add -q dos2unix
RUN apk -v --update add python py-pip parallel
RUN pip install --upgrade pyasn1-modules awscli

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

	
COPY deploy-ggs.sh /usr/local/ggs/

RUN dos2unix /usr/local/ggs/deploy-ggs.sh \
    && chmod 755 /usr/local/ggs/deploy-ggs.sh \
    && chmod 755 /usr/local/ggs/cli/cli.sh \
    && /usr/local/ggs/cli/cli.sh deploy --c /usr/local/ggs/resources --d $CATALINA_HOME/webapps/geocode --l $CATALINA_HOME/logs/ --m WAR_EXTRACTED

CMD ["/usr/local/ggs/deploy-ggs.sh"]



