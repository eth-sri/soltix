FROM ubuntu:18.10

RUN rm /var/lib/apt/lists/* -vf
RUN apt-get update && apt-get dist-upgrade -y

RUN apt-get -y install wget
RUN apt-get -y install nodejs
RUN apt-get -y install npm
RUN apt-get -y install git
RUN npm install -g n
RUN n stable

# TODO document openjdk usage
RUN apt-get -y install --fix-missing openjdk-11-jdk
RUN apt-get -y install maven

WORKDIR /soltix
COPY . /soltix

RUN ./setup.sh --use-defaults

ENTRYPOINT ["/soltix/docker-entrypoint.sh"]
