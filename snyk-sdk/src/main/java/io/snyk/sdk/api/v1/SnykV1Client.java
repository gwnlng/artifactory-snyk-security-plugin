package io.snyk.sdk.api.v1;

import javax.net.ssl.SSLContext;
import javax.net.ssl.TrustManager;
import javax.net.ssl.X509TrustManager;
import java.io.IOException;
import java.net.URLEncoder;
import java.net.InetSocketAddress;
import java.net.ProxySelector;
import java.net.http.HttpClient;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;
import java.security.SecureRandom;
import java.util.Optional;

import io.snyk.sdk.api.SnykClient;
import io.snyk.sdk.api.SnykHttpRequestBuilder;
import io.snyk.sdk.api.SnykResult;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import static java.nio.charset.StandardCharsets.UTF_8;

import io.snyk.sdk.SnykConfig;
import io.snyk.sdk.config.SSLConfiguration;
import io.snyk.sdk.model.NotificationSettings;
import io.snyk.sdk.model.TestResult;

public class SnykV1Client extends SnykClient {

  public SnykV1Client(SnykConfig config) throws Exception {
    super(config);
  }

  public SnykResult<TestResult> testMaven(String groupId, String artifactId, String version, Optional<String> organisation, Optional<String> repository) throws IOException, InterruptedException {
    HttpRequest request = SnykHttpRequestBuilder.create(config)
      .withPath(String.format(
        "test/maven/%s/%s/%s",
        URLEncoder.encode(groupId, UTF_8),
        URLEncoder.encode(artifactId, UTF_8),
        URLEncoder.encode(version, UTF_8)
      ))
      .withQueryParam("org", organisation)
      .withQueryParam("repository", repository)
      .withQueryParam("topLevelOnly", "true")
      .build();
    HttpResponse<String> response = httpClient.send(request, HttpResponse.BodyHandlers.ofString());
    return SnykResult.createResult(response, TestResult.class);
  }

  public SnykResult<TestResult> testNpm(String packageName, String version, Optional<String> organisation) throws IOException, InterruptedException {
    HttpRequest request = SnykHttpRequestBuilder.create(config)
      .withPath(String.format(
        "test/npm/%s/%s",
        URLEncoder.encode(packageName, UTF_8),
        URLEncoder.encode(version, UTF_8)
      ))
      .withQueryParam("org", organisation)
      .withQueryParam("topLevelOnly", "true")
      .build();
    HttpResponse<String> response = httpClient.send(request, HttpResponse.BodyHandlers.ofString());
    return SnykResult.createResult(response, TestResult.class);
  }

  public SnykResult<TestResult> testRubyGems(String gemName, String version, Optional<String> organisation) throws IOException, InterruptedException {
    HttpRequest request = SnykHttpRequestBuilder.create(config)
      .withPath(String.format(
        "test/rubygems/%s/%s",
        URLEncoder.encode(gemName, UTF_8),
        URLEncoder.encode(version, UTF_8)
      ))
      .withQueryParam("org", organisation)
      .withQueryParam("topLevelOnly", "true")
      .build();
    HttpResponse<String> response = httpClient.send(request, HttpResponse.BodyHandlers.ofString());
    return SnykResult.createResult(response, TestResult.class);
  }

  public SnykResult<TestResult> testPip(String packageName, String version, Optional<String> organisation) throws IOException, InterruptedException {
    HttpRequest request = SnykHttpRequestBuilder.create(config)
      .withPath(String.format(
        "test/pip/%s/%s",
        URLEncoder.encode(packageName, UTF_8),
        URLEncoder.encode(version, UTF_8)
      ))
      .withQueryParam("org", organisation)
      .withQueryParam("topLevelOnly", "true")
      .build();
    HttpResponse<String> response = httpClient.send(request, HttpResponse.BodyHandlers.ofString());
    return SnykResult.createResult(response, TestResult.class);
  }
}
