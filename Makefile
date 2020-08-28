


# Kubernetes Resources Path
KUBE_RES_PATH := ./src/main/kubernetes

# Kube Resources
JENKINS_DEPLOYMENT := ${KUBE_RES_PATH}/deployment.yaml
JENKINS_SERVICE := ${KUBE_RES_PATH}/service.yaml

build:
	mvn clean install

coverage:
	mvn cobertura:cobertura

e2e:
	kubectl create -f $(JENKINS_DEPLOYMENT)

