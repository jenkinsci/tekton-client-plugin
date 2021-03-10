


# Kubernetes Resources Path
KUBE_RES_PATH := ./src/main/kubernetes

# Kube Resources
JENKINS_DEPLOYMENT := ${KUBE_RES_PATH}/deployment.yaml
JENKINS_SERVICE := ${KUBE_RES_PATH}/service.yaml

test:
	mvn test

install-tekton:
	kubectl apply --filename https://storage.googleapis.com/tekton-releases/pipeline/latest/release.yaml

coverage:
	mvn cobertura:cobertura -q

build:
	mvn clean install -DskipTests -q

e2e:
	kubectl create -f $(JENKINS_DEPLOYMENT)

