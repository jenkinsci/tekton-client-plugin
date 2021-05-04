# Tekton Client Plugin Tutorial

Once the Tekton Client plugin is installed on your Jenkins instance we can get started by creating a new Jenkins
Job for testing the plugin out.

- Create a new job.
- During the Job Configuration, scroll down to the _Build_ Header and in the dropdown you should be able to find steps which
start with _Tekton:_.

### Create Resources 

- After going above in the dropdown, choose "Tekton: Create Resource".
- You can create resources of type Task, TaskRun, Pipeline, PipelineRun or PipelineResource.
- Choose your method of creation _(Input Type)_, `URL` or `YAML` and add your data _(Input)_ which includes the Url or YAML definition.
- Add as may steps as you want for creation of resources and "Save & Apply" the config.

Once you instantiate a build, you should be able to see your resources created.

### Delete Resources

- After going above in the dropdown, choose "Tekton: Delete Resource".
- In the Tekton Resource Type choose the kind of resource you would like to delete. Either of Task, TaskRun, Pipeline, PipelineRun or PipelineResource.
- After that put the name of the particular resource.
- Add as may steps as you want for deletion of resources and "Save & Apply" the config.

Once you instantiate a build, you should be able to see that the resources you have mentioned, get deleted.

## Usage inside a pipeline

The `Create Raw` step can be used inside a pipeline as follows:

```groovy
pipeline {
  agent any
  stages {
    stage('Stage') {
      steps {
        checkout scm
        tektonCreateRaw(inputType: 'FILE', input: '.tekton/pipeline.yaml')
      }
    }
  }
}
```

When used in this way, the following parameters are passed to the `PipelineRun` so that the correct source code can be cloned in the tekton pipeline:

* `BUILD_ID` - the build id/number of the Jenkins job
* `JOB_NAME` - ???
* `JOB_SPEC` - ???
* `JOB_TYPE` - ???
* `PULL_BASE_REF` - ???
* `PULL_BASE_SHA` - the commit sha of the base of the pull request 
* `PULL_NUMBER` - ???
* `PULL_PULL_REF` - ???
* `PULL_PULL_SHA` - ???
* `PULL_REFS` - ???
* `REPO_NAME` - ???
* `REPO_OWNER` - ???
* `REPO_URL` - the URL of the repository
