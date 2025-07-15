


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
	./scripts/e2e-test.sh

e2e-fast:
	./scripts/e2e-test.sh --fast

e2e-setup:
	./scripts/e2e-test.sh --build-only

e2e-run:
	./scripts/e2e-test.sh --test-only

e2e-cleanup:
	kind delete cluster --name tekton-e2e-test || true

# Legacy e2e target (basic deployment)
e2e-deploy:
	kubectl create -f $(JENKINS_DEPLOYMENT)

