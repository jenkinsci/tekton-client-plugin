# Developing

## Getting Started

### Prerequisites
- Basic understanding of [Plugin Development in Jenkins](https://www.jenkins.io/doc/developer/plugin-development/).

### Useful articles
- [Tutorial: Developing Complex Plugins for Jenkins](https://medium.com/velotio-perspectives/tutorial-developing-complex-plugins-for-jenkins-a34c0f979ca4) 

## Running and testing the plugin locally

Currently the plugin is in development state and a lot of things such as unit tests and such have to be added. These will be added as time passes.


- _The user needs to login to their Kubernetes Cluster before they start the following._

Currently the plugin uses the default Kubernetes Client available to it through it the local kubeconfig and operates at the current local kubecontext available during development. 

- _Use the following command to start a instance of Jenkins locally with the plugin installed_
```
mvn hpi:run
```

Ideally Jenkins should be available at **localhost:8080/jenkins**

#### Playing around
Visit [the tutorial](docs/tutorial.md) for help with doing various things with the plugin.