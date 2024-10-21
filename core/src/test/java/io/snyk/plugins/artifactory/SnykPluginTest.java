package io.snyk.plugins.artifactory;

import com.google.api.client.http.*;
import com.google.api.client.testing.http.*;
import com.google.api.client.util.ExponentialBackOff;
import io.snyk.plugins.artifactory.exception.SnykRuntimeException;
import io.snyk.plugins.artifactory.util.SnykConfigForTests;
import io.snyk.sdk.SnykConfig;
import io.snyk.sdk.api.SnykResult;
import io.snyk.sdk.model.NotificationSettings;
import org.apache.commons.io.IOUtils;
import org.junit.jupiter.api.Test;

import java.io.IOException;
import java.util.List;
import java.util.concurrent.TimeUnit;

import static org.junit.jupiter.api.Assertions.*;
import static org.awaitility.Awaitility.*;

public class SnykPluginTest {

  private final String SNYK_V1_TEST_API_URL = "https://api.snyk.io/v1/test/npm/ms/0.7.0";

//  @Test
//  void testSanitizeHeadersShouldScrubToken() throws IOException {
//    SnykConfig config = SnykConfigForTests.withDefaults();
//    var request = SnykHttpRequestBuilder.create(config);
//    String sanitizedHeaders = SnykPlugin.sanitizeHeaders(request.build());
//
//    assertFalse(sanitizedHeaders.contains(config.token));
//  }

  @Test
  public void handleThrottledResponse() throws IOException {
    SnykConfig config = SnykConfigForTests.withDefaults();
    SnykPlugin plugin = new SnykPlugin();

    //mocking httpResponse and linking to httpRequest's execution
    HttpTransport transport =
      new MockHttpTransport() {
        @Override
        public LowLevelHttpRequest buildRequest(String method, String url) {
          return new MockLowLevelHttpRequest() {
            @Override
            public LowLevelHttpResponse execute() {
              MockLowLevelHttpResponse result = new MockLowLevelHttpResponse();
              result.setContentEncoding("UTF-8");
              result.setHeaderNames(List.of("Content-Type", "Authorization"));
              result.setHeaderValues(List.of("application/json", "token " + config.token));
              result.setStatusCode(429);
              return result;
            }
          };
        }
      };

    // String testSnykOrgId = System.getenv("TEST_SNYK_ORG_ID");
    HttpRequest httpRequest = transport.createRequestFactory()
      .buildGetRequest(new GenericUrl(SNYK_V1_TEST_API_URL));
    // set backoff
    ExponentialBackOff backoff = new ExponentialBackOff.Builder()
      .setInitialIntervalMillis(500)
      .setMaxElapsedTimeMillis(120000)
      .setMaxIntervalMillis(60000)
      .setMultiplier(2)
      .setRandomizationFactor(0.5)
      .build();
    HttpBackOffUnsuccessfulResponseHandler backOffHandler = new HttpBackOffUnsuccessfulResponseHandler(backoff);
    backOffHandler.setBackOffRequired(HttpBackOffUnsuccessfulResponseHandler.BackOffRequired.ALWAYS);
    httpRequest.setUnsuccessfulResponseHandler(backOffHandler);
    // set an arbitrary retries to exceed 2 minutes max elapsed
    httpRequest.setNumberOfRetries(10);
    // eventually will still throw 429, how to handle?
    try {
      HttpResponse httpResponse = httpRequest.execute();
      given().ignoreException(HttpResponseException.class).await().atLeast(2, TimeUnit.MINUTES);
      var res = new SnykResult<NotificationSettings>(httpResponse);
      // eventually will still throw 429
      assertThrows(SnykRuntimeException.class, () -> plugin.handleResponse(res));
    } catch (HttpResponseException hre) {
      hre.printStackTrace();
    }
  }

  @Test
  public void handleSuccessResponse() throws IOException {
    SnykPlugin plugin = new SnykPlugin();

    //mocking httpResponse and linking to httpRequest's execution
    HttpTransport transport =
      new MockHttpTransport() {
        @Override
        public LowLevelHttpRequest buildRequest(String method, String url) {
          return new MockLowLevelHttpRequest() {
            @Override
            public LowLevelHttpResponse execute() {
              MockLowLevelHttpResponse result = new MockLowLevelHttpResponse();
              result.setContent("responseBody");
              result.setContentEncoding("UTF-8");
              result.setHeaderNames(List.of("header1","header2"));
              result.setHeaderValues(List.of("header1","header2"));
              result.setStatusCode(200);
              return result;
            }
          };
        }
      };
    HttpRequest httpRequest = transport.createRequestFactory()
      .buildGetRequest(new GenericUrl(SNYK_V1_TEST_API_URL));
    //getting httpResponse from httpRequest
    HttpResponse httpResponse = httpRequest.execute();
    var res = new SnykResult<NotificationSettings>(httpResponse);

    //condition to verify the content (body) of the response
    assertEquals("responseBody", IOUtils.toString(httpResponse.getContent()));

    assertDoesNotThrow(() -> plugin.handleResponse(res));
  }
}
