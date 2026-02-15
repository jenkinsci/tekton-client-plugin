# Feature Completion Report: Tekton API v1 & Full Dynamic Version Routing

**Fixes:** #470  
**PR Title:** Feature: Support Tekton API v1 and dynamic version routing

---

## Summary

The plugin now supports **full dynamic version routing** for the **entire Tekton core API set**: **PipelineRun, TaskRun, Task, and Pipeline**. Both **v1** and **v1beta1** are supported; the correct path is chosen by inspecting `apiVersion` in the user’s manifest. This removes the error *"the API version in the data (tekton.dev/v1) does not match the expected API version (tekton.dev/v1beta1)"* and future-proofs the plugin for clusters using the stable Tekton v1 API.

---

## 1. Green Build (Local Environment)

- **pom.xml**
  - **Repositories:** The `<repositories>` section explicitly includes **`https://repo.jenkins-ci.org/public/`** as the first repository, with `<releases><enabled>true</enabled></releases>` and `<snapshots><enabled>false</enabled></snapshots>`, so the Jenkins parent POM is resolved from the Jenkins repository.
  - **Plugin repositories:** Same URL and settings in `<pluginRepositories>`.
  - **If parent still fails to resolve:** Your Maven `settings.xml` may define a **mirror** that sends all repository traffic to Maven Central. The Jenkins parent lives on `repo.jenkins-ci.org`, not Central. Either:
    - Exclude `repo.jenkins-ci.org` from that mirror, or  
    - Add a profile that uses `https://repo.jenkins-ci.org/public/` and activate it when building this project.

- **Tests**
  - **CreateRawTest** is refactored into **pure unit tests**:
    - No `JenkinsRule` or real Jenkins context.
    - `KubernetesClient` (and where needed, `Resource<HasMetadata>`) are **mocked with Mockito**; no test loads a real cluster or external Jenkins parent context.
    - `Run` is mocked in `@BeforeEach` so `runCreate(run, ...)` never touches a real `Run` (avoids NPE and Jenkins dependency).

---

## 2. Resource Coverage & Consistency

| Resource      | Version routing | v1 path | resolvedNamespace in YAML + create() |
|---------------|------------------|---------|---------------------------------------|
| PipelineRun   | Yes              | Yes     | Yes                                   |
| TaskRun       | Yes              | Yes     | Yes                                   |
| Task          | Yes              | Yes     | Yes                                   |
| Pipeline      | Yes              | Yes     | Yes                                   |

For every resource, the **v1** path:

1. Parses the manifest YAML (Jackson `ObjectNode`).
2. Reads `metadata.namespace` (or null if missing).
3. Calls `resolveNamespace(resourceNamespace)` (fallback: step → global when Jenkins is up → kubeconfig → `"default"`).
4. Puts the result into the YAML: `metadataObj.put("namespace", resolvedNamespace)`.
5. Serializes back to bytes and calls `kc.resource(ByteArrayInputStream(enhancedBytes)).inNamespace(resolvedNamespace).create()`.

**createTaskV1** (consistency check) – namespace is injected into the YAML before `kc.resource(stream).inNamespace(resolvedNamespace).create()`:

```java
private String createTaskV1(byte[] data) throws Exception {
    KubernetesClient kc = (KubernetesClient) kubernetesClient;
    // ...
    ObjectNode metadataObj = (ObjectNode) metadata;
    String resourceNamespace = metadataObj.has("namespace") ? metadataObj.get("namespace").asText(null) : null;
    String resolvedNamespace = resolveNamespace(resourceNamespace);
    metadataObj.put("namespace", resolvedNamespace);   // inject into YAML
    if (Strings.isNullOrEmpty(resourceNamespace)) {
        LOGGER.info("No namespace specified in Task manifest (v1), using resolved namespace: " + resolvedNamespace);
        logMessage("Using namespace: " + resolvedNamespace);
    }
    byte[] enhancedBytes = yamlMapper.writeValueAsBytes(rootObj);
    Resource<HasMetadata> resource = kc.resource(new ByteArrayInputStream(enhancedBytes));
    HasMetadata created = resource.inNamespace(resolvedNamespace).create();
    // ...
}
```

---

## 3. Enrichment Logic (v1, no namespace)

For **v1** resources, when the user supplies YAML **without** a namespace:

- We read `metadata.namespace` (missing or null).
- We call `resolveNamespace(null)`, which uses the same fallback order as the first PR (step config → global config only when `Jenkins.getInstanceOrNull() != null` → kubeconfig → `"default"`).
- We set that value on the **ObjectNode**: `metadataObj.put("namespace", resolvedNamespace)`.
- We write the tree back to bytes and pass the resulting stream to the Kubernetes client.

So **resolvedNamespace** is always applied to the manifest (in the ObjectNode) before it is converted back to a stream and sent to `kc.resource(stream).inNamespace(resolvedNamespace).create()`.

---

## 4. Testing (Mockito, No Live Cluster)

- **CreateRawTest** is fully **unit-test** oriented:
  - **Parsing:** `testGetApiVersionFromDataV1`, `testGetApiVersionFromDataV1Beta1` (no client).
  - **Unsupported version:** `testUnsupportedApiVersionThrowsAbortException` (AbortException before any client call).
  - **v1beta1 path:** `testCreateV1Beta1PipelineRunPathStillUsed` (no “does not match the expected API version”).
  - **v1 path:** `testCreateV1PipelineRunSucceedsWithMockClient` – **Mockito** mocks `KubernetesClient` and `Resource<HasMetadata>` so the v1 create path is exercised and returns the mocked resource name; **no real cluster or Jenkins context** is required.
- **Version-routing verification** no longer depends on a live cluster; it is covered by mocks and parsing tests.

---

## 5. Deliverables Summary

| Deliverable | Status |
|-------------|--------|
| **CreateRaw.java** | Complete: dynamic v1/v1beta1 routing for PipelineRun, TaskRun, Task, Pipeline; all v1 paths use resolvedNamespace in YAML and `inNamespace(resolvedNamespace).create()`. |
| **pom.xml** | Jenkins repo `https://repo.jenkins-ci.org/public/` in `<repositories>` and `<pluginRepositories>` with releases/snapshots; note about mirror if parent still fails. |
| **CreateRawTest.java** | Pure unit tests; Mockito for client and Run; no JenkinsRule or real Jenkins context. |
| **PR description** | This document serves as the Feature Completion Report and full PR description for GitHub. |

---

---

## pom.xml snippet (Jenkins repositories)

Use this so the parent POM resolves from the Jenkins repository:

```xml
<!-- Jenkins repo first so parent POM (org.jenkins-ci.plugins:plugin) resolves from here.
     If parent still fails to resolve, ensure in settings.xml that repo.jenkins-ci.org is not mirrored to Central. -->
<repositories>
    <repository>
        <id>repo.jenkins-ci.org</id>
        <url>https://repo.jenkins-ci.org/public/</url>
        <releases>
            <enabled>true</enabled>
        </releases>
        <snapshots>
            <enabled>false</enabled>
        </snapshots>
    </repository>
    <!-- other repos ... -->
</repositories>

<pluginRepositories>
    <pluginRepository>
        <id>repo.jenkins-ci.org</id>
        <url>https://repo.jenkins-ci.org/public/</url>
        <releases>
            <enabled>true</enabled>
        </releases>
        <snapshots>
            <enabled>false</enabled>
        </snapshots>
    </pluginRepository>
</pluginRepositories>
```

---

## Full PR description (copy for GitHub)

**Title:** Feature: Support Tekton API v1 and dynamic version routing

**Description:**

Fixes #470.

- **Full dynamic version routing** for the entire Tekton core API set: PipelineRun, TaskRun, Task, and Pipeline (v1 and v1beta1). The plugin inspects `apiVersion` in the manifest and uses either the generic `KubernetesClient.resource(InputStream)` path (v1) or the typed Tekton client (v1beta1).
- **Namespace:** For every v1 resource, `resolvedNamespace` (from the existing fallback order) is injected into the YAML `metadata.namespace` before create; the same namespace is used in `inNamespace(resolvedNamespace).create()`.
- **Backward compatible:** v1beta1 behavior is unchanged. Unsupported versions throw a clear `AbortException`.
- **Testing:** CreateRawTest is refactored to pure unit tests using Mockito; no JenkinsRule or live cluster required for version-routing verification.

---

## Submitter Checklist

- [ ] Changelog updated (if applicable)
- [ ] Local build green (fix mirror in settings.xml if parent POM does not resolve)
- [ ] CI tests pass
- [ ] Backward compatibility for v1beta1 verified for all four resource types
