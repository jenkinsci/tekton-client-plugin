FROM jenkins/jenkins:2.516.3-lts-jdk21

COPY plugins.txt /usr/share/jenkins/ref/plugins.txt
RUN jenkins-plugin-cli -f /usr/share/jenkins/ref/plugins.txt --verbose

COPY target/tekton-client.hpi /usr/share/jenkins/ref/plugins
