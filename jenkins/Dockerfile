FROM jenkins/jenkins:lts-jdk21
USER root
RUN dpkg --configure -a && apt-get update && apt-get install -y docker.io maven
USER jenkins