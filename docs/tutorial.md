# Tekton Client Plugin Tutorial

Once the Tekton Client plugin is installed on your Jenkins instance we can get started by creating a new Jenkins
Job for testing the plugin out.

- Create a new job.
- During the Job Configuration, scroll down to the _Build_ Header and in the dropdown you should be able to find steps which
start with _Tekton:_.

### Create Resources 

- After going above in the dropdown, choose "Tekton: Create Resource".
- You can create resources of type Task, TaskRun, Pipeline or PipelineRun.
- Choose your method of creation _(Input Type)_, `URL` or `YAML` and add your data _(Input)_ which includes the Url or YAML definition.
- Add as may steps as you want for creation of resources and "Save & Apply" the config.

Once you instantiate a build, you should be able to see your resources created.

### Delete Resources

- After going above in the dropdown, choose "Tekton: Delete Resource".
- In the Tekton Resource Type choose the kind of resource you would like to delete. Either of Task, TaskRun, Pipeline or PipelineRun.
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

When used in this way, the following parameters are passed to the `PipelineRun` so that the 
correct source code can be cloned in the tekton pipeline:

* `BUILD_ID` - the build id/number of the Jenkins job
* `JOB_NAME` - the name of the jenkins job that triggered this pipeline
* `PULL_BASE_REF` - name of the base branch
* `PULL_PULL_SHA` - the commit sha of the pull request or branch
* `REPO_NAME` - name of the repository
* `REPO_OWNER` - owner of the repository
* `REPO_URL` - the URL of the repository

## Using the git-clone task from the tekton catalog.

To use tasks from the tekton-catalog, the tasks will need to be installed in the same namespace 
that tekton is running, once that is done they can be used in a `PipelineRun`.  An example pipeline
showing this in use is shown below:

```
apiVersion: tekton.dev/v1beta1
kind: PipelineRun
metadata:
  generateName: hello-world-pipeline-
spec:
  workspaces:
    - name: shared-data
      volumeClaimTemplate:
        spec:
          accessModes:
            - ReadWriteOnce
          resources:
            requests:
              storage: 500Mi
  pipelineSpec:
    params:
      - description: the unique build number
        name: BUILD_ID
        type: string
      - description: the git sha of the tip of the pull request
        name: PULL_PULL_SHA
        type: string
      - description: git url to clone
        name: REPO_URL
        type: string
    workspaces:
      - name: shared-data
    tasks:
      - name: fetch-repo
        taskRef:
          name: git-clone
        workspaces:
          - name: output
            workspace: shared-data
        params:
          - name: url
            value: $(params.REPO_URL)
          - name: revision
            value: $(params.PULL_PULL_SHA)
```            

to reuse the same workspace in future tasks you need to make use of the `runAfter` command e.g.:

```
      - name: do-something-with-the-source-code
        runAfter:
          - fetch-repo
        workspaces:
          - name: source
            workspace: shared-data
```            
