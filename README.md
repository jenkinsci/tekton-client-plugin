# Tekton Client Plugin
[![Build Status](https://ci.jenkins.io/job/Plugins/job/tekton-client-plugin/job/master/badge/icon)](https://ci.jenkins.io/job/Plugins/job/tekton-client-plugin/job/master/)
[![Contributors](https://img.shields.io/github/contributors/jenkinsci/tekton-client-plugin.svg)](https://github.com/jenkinsci/tekton-client-plugin/graphs/contributors)
[![Jenkins Plugin](https://img.shields.io/jenkins/plugin/v/tekton-client.svg)](https://plugins.jenkins.io/tekton-client)
[![GitHub release](https://img.shields.io/github/release/jenkinsci/tekton-client-plugin.svg?label=changelog)](https://github.com/jenkinsci/tekton-client-plugin/releases/latest)
[![Jenkins Plugin Installs](https://img.shields.io/jenkins/plugin/i/tekton-client.svg?color=blue)](https://plugins.jenkins.io/tekton-client)
[![Codecov](https://codecov.io/gh/jenkinsci/tekton-client-plugin/branch/master/graph/badge.svg)](https://codecov.io/gh/jenkinsci/tekton-client-plugin)

Jenkins plugin to interact with [Tekton Pipelines](https://github.com/tektoncd/pipeline) on Kubernetes clusters. Create, manage, and monitor Tekton resources directly from Jenkins pipelines with real-time log streaming and comprehensive integration capabilities.

## Features

### Resource Management
- **Create Resources**: Tasks, TaskRuns, Pipelines, and PipelineRuns from YAML (URLs, files, or inline)
- **Custom TaskRuns**: Build TaskRuns programmatically with task references, workspaces, and parameters  
- **Delete Resources**: Individual resources by name or bulk deletion by type
- **Multi-cluster Support**: Manage resources across multiple Kubernetes clusters

### Jenkins Integration
- **Environment Variables**: Automatic mapping of Jenkins build context to Tekton parameters
- **Real-time Logs**: Live streaming of Tekton execution logs to Jenkins console
- **GitHub Checks**: Automatic status reporting for PipelineRuns
- **Tekton Catalog**: Resolve external task references using `uses:` syntax

## Prerequisites

- Jenkins 2.492.3 or later
- Kubernetes cluster with Tekton Pipelines v0.44.0+
- Appropriate RBAC permissions for Jenkins to access Tekton resources

## Installation

**Via Plugin Manager**: Go to **Manage Jenkins** → **Manage Plugins** → Search "Tekton Client"

**Manual**: Download `.hpi` from [releases](https://github.com/jenkinsci/tekton-client-plugin/releases) and upload via **Advanced** tab

## Pipeline Steps

### `tektonCreateRaw`
Create Tekton resources from YAML definitions
- `inputType`: `'FILE'`, `'URL'`, or `'YAML'`
- `input`: File path, URL, or inline YAML content

### `tektonCreateCustomTaskRun` 
Create TaskRuns programmatically
- `taskName`: Reference to existing Task
- `taskRunName`: Name for new TaskRun
- `params`: Parameter list
- `workspaces`: Workspace bindings

### `tektonDeleteRaw`
Delete Tekton resources
- `resourceType`: `'task'`, `'taskrun'`, `'pipeline'`, `'pipelinerun'`
- `resourceName`: Specific resource (optional, deletes all if omitted)

## Configuration

**Global Settings**: **Manage Jenkins** → **Configure System** → **Tekton Client Configuration**
- Kubernetes cluster URL and credentials
- Default namespace
- Enable Tekton Catalog processing

**Environment Variable Mapping**: Jenkins variables automatically map to Tekton parameters:
`BUILD_ID` → `BUILD_ID`, `GIT_COMMIT` → `PULL_PULL_SHA`, `GIT_URL` → `REPO_URL/REPO_OWNER/REPO_NAME`

## Demo
[![](https://img.youtube.com/vi/hAWOlJ0CetQ/0.jpg)](https://www.youtube.com/watch?v=hAWOlJ0CetQ "Tekton Client Plugin")  

## Support & Community
- **Chat**: [![Gitter](https://badges.gitter.im/jenkinsci/tekton-client-plugin.svg)](https://gitter.im/jenkinsci/tekton-client-plugin)
- **Documentation**: [Tutorial](docs/tutorial.md) | [Installation Guide](docs/installation.md)
- **Issues**: [GitHub Issues](https://github.com/jenkinsci/tekton-client-plugin/issues)
- **Roadmap**: [Future Plans](roadmap.md)

## Contributing
- [DEVELOPMENT.md](DEVELOPMENT.md) - Setup and development guidelines
- [CONTRIBUTING.md](CONTRIBUTING.md) - Contribution process overview