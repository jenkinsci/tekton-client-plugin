FROM jenkins/jenkins:2.277.1-lts-jdk11

COPY plugins.txt /usr/share/jenkins/ref/plugins.txt
RUN jenkins-plugin-cli -f /usr/share/jenkins/ref/plugins.txt --verbose

COPY target/tekton-client.hpi /usr/share/jenkins/ref/plugins
