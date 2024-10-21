package io.snyk.sdk.api;

import com.google.api.client.http.HttpBackOffUnsuccessfulResponseHandler;
import com.google.api.client.http.HttpRequest;
import com.google.api.client.http.HttpResponse;
import com.google.api.client.http.javanet.NetHttpTransport;
import com.google.api.client.util.ExponentialBackOff;
import io.snyk.sdk.SnykConfig;
import io.snyk.sdk.config.SSLConfiguration;
import io.snyk.sdk.model.NotificationSettings;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import javax.net.ssl.SSLContext;
import javax.net.ssl.TrustManager;
import javax.net.ssl.X509TrustManager;
import java.io.IOException;
import java.net.InetSocketAddress;
import java.net.Proxy;
// import java.net.ProxySelector;
import java.net.URLEncoder;
// import java.net.http.HttpClient;
// import java.net.http.HttpRequest;
// import java.net.http.HttpResponse;
import java.security.SecureRandom;

import static java.nio.charset.StandardCharsets.UTF_8;

public abstract class SnykClient {
  protected static final Logger LOG = LoggerFactory.getLogger(SnykClient.class);

  protected final SnykConfig config;
  //protected final HttpClient httpClient;
  protected NetHttpTransport netHttpTransport;
  protected ExponentialBackOff backoff = new ExponentialBackOff.Builder()
    .setInitialIntervalMillis(500)
    .setMaxElapsedTimeMillis(900000)
    .setMaxIntervalMillis(120000)
    .setMultiplier(2)
    .setRandomizationFactor(0.5)
    .build();

  public SnykClient(SnykConfig config) throws Exception {
    this.config = config;

//    var builder = HttpClient.newBuilder()
//      .version(HttpClient.Version.HTTP_1_1)
//      .connectTimeout(config.timeout);

    if (config.trustAllCertificates) {
      SSLContext sslContext = SSLContext.getInstance("TLSv1.2");
      TrustManager[] trustManagers = SSLConfiguration.buildUnsafeTrustManager();
      sslContext.init(null, trustManagers, new SecureRandom());
      // builder.sslContext(sslContext);
      Proxy proxy = !config.httpProxyHost.isBlank() ? new Proxy(Proxy.Type.HTTP, new InetSocketAddress(config.httpProxyHost, config.httpProxyPort)) : null;
      LOG.info(!config.httpProxyHost.isBlank() ? "Snyk setting proxy to: " + proxy : "No proxy set");
      netHttpTransport = new NetHttpTransport.Builder()
        .setSslSocketFactory(sslContext.getSocketFactory())
        .setProxy(proxy)
        .build();
    } else if (config.sslCertificatePath != null && !config.sslCertificatePath.isEmpty()) {
      SSLContext sslContext = SSLContext.getInstance("TLSv1.2");
      X509TrustManager trustManager = SSLConfiguration.buildCustomTrustManager(config.sslCertificatePath);
      sslContext.init(null, new TrustManager[]{trustManager}, null);
      Proxy proxy = !config.httpProxyHost.isBlank() ? new Proxy(Proxy.Type.HTTP, new InetSocketAddress(config.httpProxyHost, config.httpProxyPort)) : null;
      LOG.info(!config.httpProxyHost.isBlank() ? "Snyk setting proxy to: " + proxy : "No proxy set");
      // builder.sslContext(sslContext);
      netHttpTransport = new NetHttpTransport.Builder()
        .setSslSocketFactory(sslContext.getSocketFactory())
        .setProxy(proxy)
        .build();
    }

//    if (!config.httpProxyHost.isBlank()) {
//      builder.proxy(ProxySelector.of(new InetSocketAddress(config.httpProxyHost, config.httpProxyPort)));
//      LOG.info("added proxy with ", config.httpProxyHost, config.httpProxyPort);
//    }

    // httpClient = builder.build();
  }

  public SnykResult<NotificationSettings> getNotificationSettings(String org) throws IOException {
    HttpRequest request = SnykHttpRequestBuilder.create(config)
      .withPath(String.format(
        "user/me/notification-settings/org/%s",
        URLEncoder.encode(org, UTF_8)
      ))
      .build();
    // HttpResponse<String> response = httpClient.send(request, HttpResponse.BodyHandlers.ofString());
    request.setUnsuccessfulResponseHandler(new HttpBackOffUnsuccessfulResponseHandler(backoff));
    HttpResponse response = request.execute();
    return SnykResult.createResult(response, NotificationSettings.class);
  }
}
