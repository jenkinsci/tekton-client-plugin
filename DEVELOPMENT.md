# Development for Tekton Client Plugin
Tekton is a cloud-native solution for building CI/CD systems. It consists of Tekton Pipelines, which provides the building blocks, and of supporting components, such as Tekton CLI and Tekton Catalog, that make Tekton a complete ecosystem.

## Overview

### Prerequisites
- Basic understanding of [Plugin Development in Jenkins](https://www.jenkins.io/doc/developer/plugin-development/).

### Useful articles
- [Tutorial: Developing Complex Plugins for Jenkins](https://medium.com/velotio-perspectives/tutorial-developing-complex-plugins-for-jenkins-a34c0f979ca4)

## Getting Started 
### Prerequisites
1. Install [minikube](https://minikube.sigs.k8s.io/docs/start/?arch=%2Fwindows%2Fx86-64%2Fstable%2F.exe+download). You only have to complete the step 1, “Installation”.

2. Install [kubectl](https://kubernetes.io/docs/tasks/tools/#kubectl).
## Set up and run your first Tekton Task and Pipeline
1. [Create a Kubernetes cluster with minikube](#Create-a-Kubernetes-cluster).

2. [Install Tekton pipelines and Configure Jenkins](#Install-tekton-pipelines).

3. [Create and Run a Task and a Pipeline](#Create-and-Run-a-Task-and-a-Pipeline).

### Create a Kubernetes cluster
1. Create a Cluster
````
minikube start --kubernetes-version v1.30.2
````
2. You can check that the cluster was successfully created with `kubectl`:
```` 
kubectl cluster-info
````
3. The output confirms that Kubernetes is running:
````
Kubernetes control plane is running at https://127.0.0.1:39509
CoreDNS is running at
https://127.0.0.1:39509/api/v1/namespaces/kube-system/services/kube-dns:dns/proxy

To further debug and diagnose cluster problems, use 'kubectl cluster-info dump'.
````
### Install Tekton Pipelines
1. To install the latest version of Tekton Pipelines, use `kubectl`:
````
kubectl apply --filename \
https://storage.googleapis.com/tekton-releases/pipeline/latest/release.yaml
````
2. Monitor the installation 
````
kubectl get pods --namespace tekton-pipelines --watch
````
- When both `tekton-pipelines-controller` and `tekton-pipelines-webhook` show `1/1` under the `READY` column, you are ready to continue. For example:
````
NAME                                           READY   STATUS              RESTARTS   AGE
tekton-pipelines-controller-6d989cc968-j57cs   0/1     Pending             0          3s
tekton-pipelines-webhook-69744499d9-t58s5      0/1     ContainerCreating   0          3s
tekton-pipelines-controller-6d989cc968-j57cs   0/1     ContainerCreating   0          3s
tekton-pipelines-controller-6d989cc968-j57cs   0/1     Running             0          5s
tekton-pipelines-webhook-69744499d9-t58s5      0/1     Running             0          6s
tekton-pipelines-controller-6d989cc968-j57cs   1/1     Running             0          10s
tekton-pipelines-webhook-69744499d9-t58s5      1/1     Running             0          20s
````
Hit Ctrl + C to stop monitoring.
3. Install the Tekton Client Plugin in Jenkins

- In Jenkins, go to Manage Jenkins > Manage Plugins.

- Search for "Tekton Client Plugin" in the "Available" tab, install it, and restart Jenkins if required.
  
- Verify installation under Manage Jenkins > Configure System; look for Tekton-related options

### Create and Run a Task and a Pipeline 
- A Task, represented in the API as an object of kind Task, defines a series of Steps that run sequentially to perform logic that the Task requires. Every Task runs as a pod on the Kubernetes cluster, with each step running in its own container.


*Example showing how to create and run a task and a pipeline:*
1. a) To create a Task, open your preferred editor and create a file named `hello-world.yaml` with the following content:

````yaml
apiVersion: tekton.dev/v1beta1
kind: Task
metadata:
  name: hello
spec:
  steps:
    - name: echo
      image: alpine
      script: |
        #!/bin/sh
        echo "Hello World"
````
- A Pipeline defines an ordered series of Tasks arranged in a specific execution order as part of the CI/CD workflow.

1. b) Create a new file named hello-goodbye-pipeline.yaml and add the following content:
````yaml
apiVersion: tekton.dev/v1beta1
kind: Pipeline
metadata:
  name: hello-goodbye
spec:
  params:
    - name: username
      type: string
  tasks:
    - name: hello
      taskRef:
        name: hello
    - name: goodbye
      runAfter:
        - hello
      taskRef:
        name: goodbye
      params:
        - name: username
          value: $(params.username)
````

2. Apply the changes to your cluster:
````
kubectl apply --filename hello-world.yaml
````
````
kubectl apply --filename hello-goodbye-pipeline.yaml
````
- The output confirms that the Task was created successfully:
````
task.tekton.dev/hello created
````
3. A `TaskRun` and `PipelineRun` object instantiates and executes the Task and Pipeline respectively. Create another file named `hello-world-run.yaml` and `hello-goodbye-pipeline-run.yaml` with the following content:

````yaml
apiVersion: tekton.dev/v1beta1
kind: TaskRun
metadata:
  name: hello-task-run
spec:
  taskRef:
    name: hello
````
````yaml
apiVersion: tekton.dev/v1beta1
kind: PipelineRun
metadata:
  name: hello-goodbye-run
spec:
  pipelineRef:
    name: hello-goodbye
  params:
  - name: username
    value: "Tekton"
````
4. Apply the changes to your cluster to launch the Task and Pipeline:
````
kubectl apply --filename hello-world-run.yaml
````
````
kubectl apply --filename hello-goodbye-pipeline-run.yaml
````

- Verify everything worked correctly: 
````
kubectl get taskrun hello-task-run
````
````
You will see the following output for Task:
NAME                    SUCCEEDED    REASON       STARTTIME   COMPLETIONTIME
hello-task-run          True         Succeeded    22h         22h
````
- Output for pipeline
````
pipelinerun.tekton.dev/hello-goodbye-run created
````
5. Take a look at the logs:
````
kubectl logs --selector=tekton.dev/taskRun=hello-task-run
````
````
tkn pipelinerun logs hello-goodbye-run -f -n default
````
- The output displays the message:
````
[hello : echo] Hello World!

[goodbye : goodbye] Goodbye Tekton!
````

## Running and testing the plugin locally

Presently the plugin is in development state and a lot of things such as unit tests and such have to be added. These will be added as time progresses.


- _The user needs to login to their Kubernetes Cluster before they start the following._

Currently the plugin uses the default Kubernetes Client available to it through it the local kubeconfig and operates at the current local kubecontext available during development. 

- _Use the following command to start a instance of Jenkins locally with the plugin installed_
```
mvn hpi:run
```

Ideally Jenkins should be available at **localhost:8080/jenkins**

#### Other Implementations to try
Visit [the tutorial](docs/tutorial.md) for help with doing various things with the plugin.
## Releasing

Before releasing you need to run the following command:

```bash 
mvn package -P download-binaries
```

This will then download the [jx-pipeline](https://github.com/jenkins-x/jx-pipeline/releases) binaries for each platform so they can be embedded inside the plugin.
