# Installation Instructions

To install and configure this plugin we would recommend the following setup:

* Jenkins on k8s install using the jenkinsci/helm-chart
* Tekton Install in a separate namespace

## Configuring Jenkins

The recommended way of running Jenkins in a k8s cluster is to create a custom Jenkins image 
with pre-installed plugins to avoid downloading plugins each time the pod restarts.  To install 
the `tekton-client` plugin, you can add the following to your `plugins.txt`

```
tekton-client:1.0.0
```

This file can be kept up to date using tools like `jenkins-infra/uc` or `plugin-installation-manager`.

## Installing Tekton

It's recommended to install Tekton in a separate namespace to Jenkins, e.g.

```
kubectl create ns tekton-pipelines
<switch to that namespace>
kubectl apply --filename https://storage.googleapis.com/tekton-releases/pipeline/latest/release.yaml
```

If using tasks from the Tekton Catalog, they should also be installed into this namespace.

## Giving Jenkins Permission to access the Tekton Namespace

The final step is to grant the Jenkins Service Account the permission to view the required resources in the 
`tekton-pipelines` namespace.  This can be done by creating `Role` & `RoleBinding` resource e.g.

```
---
kind: Role
apiVersion: rbac.authorization.k8s.io/v1
metadata:
  name: tekton-role
  namespace: <namespace that tekton is installed>
rules:
  - apiGroups:
      - ""
    resources:
      - pods
      - pods/log
    verbs:
      - get
      - list
      - watch
  - apiGroups:
      - tekton.dev
    resources:
      - tasks
      - taskruns
      - pipelines
      - pipelineruns
    verbs:
      - create
      - delete
      - deletecollection
      - get
      - list
      - patch
      - update
      - watch
...
```

and 

```
apiVersion: rbac.authorization.k8s.io/v1
kind: RoleBinding
metadata:
  name: tekton-role-binding
  namespace: <namespace that tekton is installed>
roleRef:
  apiGroup: rbac.authorization.k8s.io
  kind: Role
  name: tekton-role
subjects:
  - kind: ServiceAccount
    name: jenkins
    namespace: <namespace that jenkins is installed>
```
