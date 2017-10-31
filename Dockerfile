FROM groovy

WORKDIR /usr/src/app
USER root
RUN apt-get update; apt-get install -y maven

COPY . .

CMD [ "groovy", "./licenses.groovy"]