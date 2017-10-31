FROM groovy:2.4-alpine

WORKDIR /usr/src/app
USER root
RUN apk add --no-cache maven

COPY . .

CMD [ "groovy", "./licenses.groovy"]