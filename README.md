# Tekton Client Plugin

[![Build Status](https://ci.jenkins.io/job/Plugins/job/tekton-client-plugin/job/master/badge/icon)](https://ci.jenkins.io/job/Plugins/job/tekton-client-plugin/job/master/)
[![Contributors](https://img.shields.io/github/contributors/jenkinsci/tekton-client-plugin.svg)](https://github.com/jenkinsci/tekton-client-plugin/graphs/contributors)
[![Jenkins Plugin](https://img.shields.io/jenkins/plugin/v/tekton-client.svg)](https://plugins.jenkins.io/tekton-client)
[![GitHub release](https://img.shields.io/github/release/jenkinsci/tekton-client-plugin.svg?label=changelog)](https://github.com/jenkinsci/tekton-client-plugin/releases/latest)
[![Jenkins Plugin Installs](https://img.shields.io/jenkins/plugin/i/tekton-client.svg?color=blue)](https://plugins.jenkins.io/tekton-client)
[![Codecov](https://codecov.io/gh/jenkinsci/tekton-client-plugin/branch/master/graph/badge.svg)](https://codecov.io/gh/jenkinsci/tekton-client-plugin)


Jenkins plugin to interact with [Tekton Pipelines](https://github.com/tektoncd/pipeline) on a Kubernetes Cluster. 

#### **Current State** : _Alpha_

## Start using the plugin

### Supported Tekton Resource Types
- Task 
- TaskRun
- Pipeline
- PipelineRun

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
