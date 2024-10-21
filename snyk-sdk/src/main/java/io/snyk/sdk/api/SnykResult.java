package io.snyk.sdk.api;

import com.fasterxml.jackson.databind.DeserializationFeature;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.google.api.client.http.HttpResponse;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.IOException;
import java.util.Optional;

public class SnykResult<T> {
  public int statusCode;
  public Optional<T> result = Optional.empty();
//  public Optional<String> responseAsText = Optional.empty();
  public HttpResponse response;
  private static final Logger LOG = LoggerFactory.getLogger(SnykResult.class);

  public SnykResult(int statusCode, T result, HttpResponse response) {
    this.statusCode = statusCode;
    this.result = Optional.of(result);
//    this.responseAsText = Optional.of(responseBody);
    this.response = response;
  }

  public SnykResult(HttpResponse response) {
    this.statusCode = response.getStatusCode();
//    this.responseAsText = Optional.of(response.body());
    this.response = response;
  }

  public Optional<T> get() {
    return this.result;
  }

  public boolean isSuccessful() {
    return statusCode == 200;
  }

  public static <ResType> SnykResult<ResType> createResult(HttpResponse response, Class<ResType> resultType) throws IOException {
    int status = response.getStatusCode();
    LOG.info("Snyk retrieving REST response status code: " + status);
    if (status == 200) {
      //String responseBody = response.body();
      ObjectMapper objectMapper = new ObjectMapper().configure(DeserializationFeature.FAIL_ON_UNKNOWN_PROPERTIES, false);
      var res = objectMapper.readValue(response.getContent(), resultType);
      LOG.debug("Snyk retrieving mapped json object:\n" + res.toString());
      return new SnykResult<>(status, res, response);
    } else {
      return new SnykResult<>(response);
    }
  }
}
