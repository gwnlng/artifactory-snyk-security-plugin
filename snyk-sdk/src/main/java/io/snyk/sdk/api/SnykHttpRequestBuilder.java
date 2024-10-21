package io.snyk.sdk.api;

import com.google.api.client.http.GenericUrl;
import com.google.api.client.http.HttpHeaders;
import com.google.api.client.http.HttpRequest;
import com.google.api.client.http.HttpRequestFactory;
import com.google.api.client.http.javanet.NetHttpTransport;
import io.snyk.sdk.SnykConfig;

import javax.annotation.Nonnull;
import java.io.IOException;
import java.net.URI;
import java.net.URLEncoder;
//import java.net.http.HttpRequest;
import java.util.HashMap;
import java.util.Optional;
import java.util.stream.Collectors;

import static java.nio.charset.StandardCharsets.UTF_8;


public class SnykHttpRequestBuilder {
  private final SnykConfig config;
  private final HashMap<String, String> queryParams = new HashMap<>();
  private String path = "";
  private final HttpRequestFactory requestFactory = new NetHttpTransport().createRequestFactory();

  private SnykHttpRequestBuilder(@Nonnull SnykConfig config) {
    this.config = config;
  }

  public static SnykHttpRequestBuilder create(@Nonnull SnykConfig config) {
    return new SnykHttpRequestBuilder(config);
  }

  public SnykHttpRequestBuilder withPath(@Nonnull String path) {
    this.path = path;
    return this;
  }

  public SnykHttpRequestBuilder withQueryParam(String key, String value) {
    return withQueryParam(key, Optional.ofNullable(value));
  }

  public SnykHttpRequestBuilder withQueryParam(String key, Optional<String> value) {
    value.ifPresent(v -> this.queryParams.put(key, v));
    return this;
  }

  public HttpRequest build() throws IOException {
    // build a GET request
    HttpRequest request = requestFactory.buildGetRequest(buildURI(config.baseUrl));
    // set headers and timeout
    request.setHeaders(new HttpHeaders()
      .set("Authorization", String.format("token %s", config.token))
      .set("User-Agent", config.userAgent)
    );
    request.setWriteTimeout((int) config.timeout.toMillis());
    return request;
//    return HttpRequest.newBuilder()
//      .GET()
//      .uri(buildURI(config.baseUrl))
//      .timeout(config.timeout)
//      .setHeader("Authorization", String.format("token %s", config.token))
//      .setHeader("User-Agent", config.userAgent)
//      .build();
  }

  public HttpRequest buildRestClient() throws IOException {
    // build a GET request
    HttpRequest request = requestFactory.buildGetRequest(buildURI(config.restBaseUrl));
    // set headers and timeout
    request.setHeaders(new HttpHeaders()
      .set("Authorization", String.format("token %s", config.token))
      .set("User-Agent", config.userAgent)
      .set("Content-Type", "application/vnd.api+json")
    );
    request.setWriteTimeout((int) config.timeout.toMillis());
    return request;
//
//    String contentType = "application/vnd.api+json";
//    return HttpRequest.newBuilder()
//      .GET()
//      .uri(buildURI(config.restBaseUrl))
//      .timeout(config.timeout)
//      .setHeader("Authorization", String.format("token %s", config.token))
//      .setHeader("User-Agent", config.userAgent)
//      .setHeader("Content-Type", contentType)
//      .build();
  }

  private GenericUrl buildURI(String baseUrl) {
    String apiUrl = baseUrl + path;

    String queryString = this.queryParams
      .entrySet()
      .stream()
      .map((entry) -> String.format(
        "%s=%s",
        URLEncoder.encode(entry.getKey(), UTF_8),
        URLEncoder.encode(entry.getValue(), UTF_8)))
      .collect(Collectors.joining("&"));

    if (!queryString.isBlank()) {
      apiUrl += "?" + queryString;
    }

//    return URI.create(apiUrl);
    return new GenericUrl(URI.create(apiUrl));
  }
}
