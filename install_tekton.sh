#!/bin/bash

cluster_name="tekton"
logs="tekton.log"
pipeline_url="https://storage.googleapis.com/tekton-releases/pipeline/latest/release.yaml"
dashboard_url="https://storage.googleapis.com/tekton-releases/dashboard/latest/release.yaml"


# Check if Minikube is already running
minikube_status=$(minikube status | grep -i running)

if [ ! -z "$minikube_status" ]; then
  echo "Minikube is already running, delete existing cluster (y/n)?"
  read resp
  if [ $resp = 'y' ]; then
     echo "Stopping Minikube"
     minikube stop
     echo "Deleting Minikube cluster"
     minikube delete
  fi
fi

echo "Starting Minikube"
minikube start --kubernetes-version v1.30.2
#minikube status

echo "Installing Tekton Pipelines"
kubectl apply --filename $pipeline_url 2>&1 > $logs
kubectl apply --filename $dashboard_url 2>&1 >> $logs
sleep 5

echo -n "Waiting for Tekton to start"
x=0
while [ $x != 3 ]
do
    x=$(kubectl -n tekton-pipelines get pods --field-selector=status.phase=Running -o jsonpath='{.items[*].status.phase}' | wc -w)
    sleep 10
    echo -n "."
done

echo "Done."

kubectl -n tekton-pipelines delete service tekton-dashboard 2>&1 > $logs
kubectl -n tekton-pipelines create service nodeport tekton-dashboard --tcp=9097:9097 --node-port=30000 2>&1 > $logs

echo "Dashboard: http://localhost:30000"
echo "$cluster_name Configuration."