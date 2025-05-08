/*
Copyright 2020 The Kubernetes Authors.
Licensed under the Apache License, Version 2.0 (the "License");
you may not use this file except in compliance with the License.
You may obtain a copy of the License at
http://www.apache.org/licenses/LICENSE-2.0
Unless required by applicable law or agreed to in writing, software
distributed under the License is distributed on an "AS IS" BASIS,
WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
See the License for the specific language governing permissions and
limitations under the License.
*/
package io.kubernetes.client;

import static com.github.tomakehurst.wiremock.client.WireMock.aResponse;
import static com.github.tomakehurst.wiremock.client.WireMock.equalTo;
import static com.github.tomakehurst.wiremock.client.WireMock.get;
import static com.github.tomakehurst.wiremock.client.WireMock.getRequestedFor;
import static com.github.tomakehurst.wiremock.client.WireMock.urlPathEqualTo;
import static com.github.tomakehurst.wiremock.core.WireMockConfiguration.options;
import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;

import com.github.tomakehurst.wiremock.core.Admin;
import com.github.tomakehurst.wiremock.extension.Parameters;
import com.github.tomakehurst.wiremock.extension.PostServeAction;
import com.github.tomakehurst.wiremock.junit5.WireMockExtension;
import com.github.tomakehurst.wiremock.stubbing.ServeEvent;
import io.kubernetes.client.Exec.ExecProcess;
import io.kubernetes.client.openapi.ApiClient;
import io.kubernetes.client.openapi.ApiException;
import io.kubernetes.client.openapi.models.V1ObjectMeta;
import io.kubernetes.client.openapi.models.V1Pod;
import io.kubernetes.client.util.ClientBuilder;
import io.kubernetes.client.util.Streams;
import java.io.ByteArrayInputStream;
import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.nio.charset.StandardCharsets;
import java.util.concurrent.CountDownLatch;
import java.util.function.Consumer;
import java.util.stream.Stream;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.RegisterExtension;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.Arguments;
import org.junit.jupiter.params.provider.MethodSource;

import static org.junit.jupiter.params.provider.Arguments.arguments;


/** Tests for the Exec helper class */
class ExecTest {

  public static class CountDownLatchAction extends PostServeAction {
    @Override
    public String getName() {
      return "countdown";
    }

    @Override
    public void doAction(ServeEvent serveEvent, Admin admin, Parameters parameters) {
      CountDownLatch latch = (CountDownLatch) parameters.get("latch");
      latch.countDown();
    }
  }

  private static final String OUTPUT_EXIT0 = "{\"metadata\":{},\"status\":\"Success\"}";
  private static final String OUTPUT_EXIT1 =
      "{\"metadata\":{},\"status\":\"Failure\",\"message\":\"command terminated with non-zero exit code: Error executing in Docker Container: 1\",\"reason\":\"NonZeroExitCode\",\"details\":{\"causes\":[{\"reason\":\"ExitCode\",\"message\":\"1\"}]}}";
  private static final String OUTPUT_EXIT126 =
      "{\"metadata\":{},\"status\":\"Failure\",\"message\":\"command terminated with non-zero exit code: Error executing in Docker Container: 126\",\"reason\":\"NonZeroExitCode\",\"details\":{\"causes\":[{\"reason\":\"ExitCode\",\"message\":\"126\"}]}}";
  private static final String BAD_OUTPUT_INCOMPLETE_MSG1 =
      "{\"metadata\":{},\"status\":\"Failure\",\"message\":\"command terminated with non-zero exit code: Error executing in Docker Container: 1\",\"reas";
  private static final String OUTPUT_EXIT_BAD_INT =
      "{\"metadata\":{},\"status\":\"Failure\",\"message\":\"command terminated with non-zero exit code: Error executing in Docker Container: 126\",\"reason\":\"NonZeroExitCode\",\"details\":{\"causes\":[{\"reason\":\"ExitCode\",\"message\":\"not a number\"}]}}";

  private static final int EXPECTED_ERROR_EXIT_CODE = -1975219;

  private String namespace;
  private String podName;
  private String[] cmd;

  private ApiClient client;

  @RegisterExtension
  static WireMockExtension apiServer =
      WireMockExtension.newInstance()
        .options(options().dynamicPort().extensions(new CountDownLatchAction()))
        .build();

  @BeforeEach
  void setup() {
    client = new ClientBuilder().setBasePath("http://localhost:" + apiServer.getPort()).build();

    namespace = "default";
    podName = "apod";
    cmd = new String[] {"sh", "-c", "echo Hello from inside the pod && ls /tmp"};
  }

  public static InputStream makeStream(int streamNum, byte[] data) {
    return makeStream(new byte[] {(byte) streamNum}, data);
  }

  public static InputStream makeStream(byte[] prefix, byte[] data) {
    byte[] out = new byte[prefix.length + data.length];
    System.arraycopy(prefix, 0, out, 0, prefix.length);
    System.arraycopy(data, 0, out, prefix.length, data.length);
    return new ByteArrayInputStream(out);
  }

  public static Thread asyncCopy(
      final InputStream is, final OutputStream os, CountDownLatch cLatch) {
    Thread t =
        new Thread(
            new Runnable() {
              public void run() {
                try {
                  Streams.copy(is, os);
                } catch (IOException ex) {
                  ex.printStackTrace();
                } finally {
                  cLatch.countDown();
                }
              }
            });
    t.start();
    return t;
  }

  @Test
  void execProcess() throws IOException, InterruptedException {
    final ExecProcess process = new ExecProcess(client);
    process.getHandler().open("wss", null);
    String msgData = "This is the stdout message";
    String errData = "This is the stderr message";

    process.getHandler().bytesMessage(makeStream(1, msgData.getBytes(StandardCharsets.UTF_8)));
    process.getHandler().bytesMessage(makeStream(2, errData.getBytes(StandardCharsets.UTF_8)));

    final ByteArrayOutputStream stdout = new ByteArrayOutputStream();
    final ByteArrayOutputStream stderr = new ByteArrayOutputStream();
    CountDownLatch cLatch = new CountDownLatch(2);
    Thread t1 = asyncCopy(process.getInputStream(), stdout, cLatch);
    Thread t2 = asyncCopy(process.getErrorStream(), stderr, cLatch);

    process.getHandler().bytesMessage(makeStream(3, OUTPUT_EXIT0.getBytes(StandardCharsets.UTF_8)));

    cLatch.await();
    process.destroy();

    assertThat(stdout).hasToString(msgData);
    assertThat(stderr).hasToString(errData);
    assertThat(process.isAlive()).isFalse();
    assertThat(process.exitValue()).isZero();
  }

  @Test
  void terminalResize() throws IOException, InterruptedException {
    final ExecProcess process = new ExecProcess(client);
    ByteArrayOutputStream bos = new ByteArrayOutputStream();

    process.getHandler().injectOutputStream(4, bos);
    process.resize(100, 100);
    process.destroy();

    String out = bos.toString("UTF-8");
    assertThat(out).isEqualTo("{ \"width\": 100, \"height\": 100 }\n");
  }

  @Test
  void defaultUnhandledError() throws IOException, InterruptedException {
    final Throwable throwable = mock(Throwable.class);
    final ExecProcess process = new ExecProcess(client);
    process.getHandler().open("wss", null);

    process.getHandler().failure(throwable);
    process.waitFor();

    verify(throwable, times(1)).printStackTrace();
    assertThat(process.isAlive()).isFalse();
    assertThat(process.exitValue()).isEqualTo(EXPECTED_ERROR_EXIT_CODE);
  }

  @Test
  void customUnhandledError() throws IOException, InterruptedException {
    final Consumer<Throwable> consumer = mock(Consumer.class);
    final Throwable throwable = mock(Throwable.class);
    final ExecProcess process = new ExecProcess(client, consumer);
    process.getHandler().open("wss", null);

    process.getHandler().failure(throwable);
    process.waitFor();

    verify(throwable, times(0)).printStackTrace();
    verify(consumer, times(1)).accept(throwable);
    assertThat(process.isAlive()).isFalse();
    assertThat(process.exitValue()).isEqualTo(EXPECTED_ERROR_EXIT_CODE);
  }

  @Test
  void url() throws IOException, ApiException, InterruptedException {
    final Consumer<Throwable> consumer = mock(Consumer.class);
    Exec exec = new Exec(client);
    exec.setOnUnhandledError(consumer);

    V1Pod pod = new V1Pod().metadata(new V1ObjectMeta().name(podName).namespace(namespace));

    CountDownLatch latch = new CountDownLatch(2);
    Parameters params = new Parameters();
    params.put("latch", latch);

    apiServer.stubFor(
        get(urlPathEqualTo("/api/v1/namespaces/" + namespace + "/pods/" + podName + "/exec"))
            .withPostServeAction("countdown", params)
            .willReturn(
                aResponse()
                    .withStatus(404)
                    .withHeader("Content-Type", "application/json")
                    .withBody("{}")));

    Process p = exec.exec(pod, cmd, "container", true, false);
    p.waitFor();

    exec.newExecutionBuilder(pod.getMetadata().getNamespace(), pod.getMetadata().getName(), cmd)
        .setContainer("container")
        .setStdin(false)
        .setStderr(false)
        .execute()
        .waitFor();
    latch.await();

    apiServer.verify(
        getRequestedFor(
                urlPathEqualTo("/api/v1/namespaces/" + namespace + "/pods/" + podName + "/exec"))
            .withQueryParam("stdin", equalTo("true"))
            .withQueryParam("stdout", equalTo("true"))
            .withQueryParam("stderr", equalTo("true"))
            .withQueryParam("container", equalTo("container"))
            .withQueryParam("tty", equalTo("false"))
            .withQueryParam("command", equalTo("sh"))
            .withQueryParam("command", equalTo("-c"))
            .withQueryParam("command", equalTo("echo Hello from inside the pod && ls /tmp")));


    apiServer.verify(
        getRequestedFor(
                urlPathEqualTo("/api/v1/namespaces/" + namespace + "/pods/" + podName + "/exec"))
            .withQueryParam("stdin", equalTo("false"))
            .withQueryParam("stdout", equalTo("true"))
            .withQueryParam("stderr", equalTo("false"))
            .withQueryParam("container", equalTo("container"))
            .withQueryParam("tty", equalTo("false"))
            .withQueryParam("command", equalTo("sh"))
            .withQueryParam("command", equalTo("-c"))
            .withQueryParam("command", equalTo("echo Hello from inside the pod && ls /tmp")));


    assertThat(p.exitValue()).isEqualTo(EXPECTED_ERROR_EXIT_CODE);
    verify(consumer, times(1)).accept(any(Throwable.class));
  }

  static Stream<Arguments> exitCodeTestData() {
	    return Stream.of(
	        arguments(OUTPUT_EXIT0, 0),
	        arguments(OUTPUT_EXIT1, 1),
	        arguments(OUTPUT_EXIT126, 126),
	        arguments(BAD_OUTPUT_INCOMPLETE_MSG1, -1),
	        arguments(OUTPUT_EXIT_BAD_INT, -1)
	    );
	}

  @ParameterizedTest
  @MethodSource("exitCodeTestData")
  void testExitCodeParsing(String output, int expectedExitCode) {
      InputStream inputStream = new ByteArrayInputStream(output.getBytes(StandardCharsets.UTF_8));
      int exitCode = Exec.parseExitCode(client, inputStream);
      assertThat(exitCode).isEqualTo(expectedExitCode);
  }

  @Test
  void executionBuilderNull() {
    Exec exec = new Exec(null);
    assertThatThrownBy(() -> {
      exec.newExecutionBuilder(null, null, null);
    }).isInstanceOf(NullPointerException.class);
    assertThatThrownBy(() -> {
      exec.newExecutionBuilder("", null, null);
    }).isInstanceOf(NullPointerException.class);
    assertThatThrownBy(() -> {
      exec.newExecutionBuilder("", "", null);
    }).isInstanceOf(NullPointerException.class);
  }
}
