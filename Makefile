


# Kubernetes Resources Path
KUBE_RES_PATH := ./src/main/kubernetes

# Kube Resources
JENKINS_DEPLOYMENT := ${KUBE_RES_PATH}/deployment.yaml
JENKINS_SERVICE := ${KUBE_RES_PATH}/service.yaml

install-tekton:
	kubectl apply --filename https://storage.googleapis.com/tekton-releases/pipeline/latest/release.yaml

coverage:
	mvn cobertura:cobertura

build:
	mvn clean install -DskipTests

e2e:
	kubectl create -f $(JENKINS_DEPLOYMENT)

