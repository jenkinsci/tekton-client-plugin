# Tekton Client Plugin

[![Build Status](https://travis-ci.com/jenkinsci/tekton-client-plugin.svg?branch=master)](https://travis-ci.com/jenkinsci/tekton-client-plugin) [![codecov](https://codecov.io/gh/jenkinsci/tekton-client-plugin/branch/master/graph/badge.svg)](https://codecov.io/gh/jenkinsci/tekton-client-plugin)


Jenkins plugin to interact with [Tekton Pipelines](https://github.com/tektoncd/pipeline) on a Kubernetes Cluster. 
#### **Current State** : _Proof of Concept_

## Start using the plugin

### Supported Tekton Resource Types
- Task 
- TaskRun
- Pipeline
- PipelineRun
- PipelineResource

### Supported Actions on Resources
- Create 
- Delete

_Currently in the tekton-client-plugin we are able to create and delete Tekton Resources._ 

- Learn more with [the tutorial!](docs/tutorial.md)
- Check out the [roadmap](roadmap.md) for a better idea of what's planned for the future.

## Want to contribute

Awesome ! Let's get started !

- Check out [DEVELOPMENT.md](DEVELOPMENT.md) for getting started with setup and jenkins plugin hackery.
- Check out [CONTRIBUTING.md](CONTRIBUTING.md) for an overview of the process.
