# Tekton-Jenkins Integration Guide

This guide provides a step-by-step tutorial for integrating Tekton, a Kubernetes-native CI/CD framework, with Jenkins, a popular automation server. It covers prerequisites, installation, configuration, and troubleshooting to help you set up and manage Tekton pipelines triggered from Jenkins. Follow this guide to streamline your CI/CD workflows effectively.

## Prerequisites
1. **Kubernetes Cluster** (choose one):
   - **Docker Desktop**:  
     - Enable Kubernetes in `Settings > Kubernetes`.  
     - Verify:  
       ```bash
       kubectl get nodes  # Expected output: `docker-desktop`
       ```
   - **Minikube**:  
     - Start the cluster:  
       ```bash
       minikube start --driver=docker
       ```
     - Verify:  
       ```bash
       kubectl get nodes  # Expected output: `minikube`
       ```

2. **Tools**:
   - Jenkins (installed via [Docker](https://www.jenkins.io/doc/book/installing/#docker) or [bare metal](https://www.jenkins.io/doc/book/installing/linux/)).
   - `kubectl` configured to access your cluster.

---

## Step-by-Step Setup
### 1. Install Tekton
```bash
kubectl apply -f https://storage.googleapis.com/tekton-releases/pipeline/latest/release.yaml
```

You may also follow the following [Installation tutorial](https://github.com/jenkinsci/tekton-client-plugin/blob/master/docs/installation.md) for installing Tekton on your system.

### 2. Configure Jenkins

**Install the Tekton Client Plugin:**
- Go to `Manage Jenkins > Manage Plugins > Available`.
- Search for "Tekton Client" and install it.

**Add Kubernetes Credentials:**
We need to add Kubernetes credentials in Jenkins because Jenkins requires:

- **A `kubeconfig` file or ServiceAccount token** to authenticate with the Kubernetes cluster.  
- **Proper RBAC permissions** to create/delete Tekton `PipelineRun` resources.

- Go to `Manage Jenkins > Manage Credentials`.
- Add a Secret file credential with your kubeconfig (`~/.kube/config`).
- To locate this kubeconfig, you may use the following command:
  ```bash
  cp ~/.kube/config /path/to/accessible/location/config
  ```
  This will copy the config file to your desired folder, allowing you to upload it.

**Configure the Plugin:**
- Go to `Manage Jenkins > Configure System`.
- Under Tekton, set:
  - **Kubernetes Cluster URL:**
    - Docker Desktop: `https://kubernetes.docker.internal:6443`
    - Minikube: `https://$(minikube ip):8443`
  - **Namespace:** `tekton-pipelines` (default for Tekton).
  - **Credentials:** Select your uploaded kubeconfig.
  
If you compleated all the above setup then you may test your integration using a simple pipline job in jenkins and try triggering your tekton pipeline.
---

### Common Issues & Fixes
| Symptom                          | Solution                                                                 |
|----------------------------------|--------------------------------------------------------------------------|
| "Credentials" field missing      | 1. Install the **Kubernetes Plugin** in Jenkins.<br>2. Restart Jenkins. |
| Cluster context mismatch         | Run: `kubectl config use-context docker-desktop` (or `minikube`).        |
| PipelineRun fails silently       | Check RBAC permissions.                                                 |
