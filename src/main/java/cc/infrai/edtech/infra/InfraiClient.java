package cc.infrai.edtech.infra;

import cc.infrai.edtech.config.AppConfig;

import java.io.IOException;
import java.net.URI;
import java.net.URLEncoder;
import java.net.http.HttpClient;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;
import java.nio.charset.StandardCharsets;
import java.time.Duration;
import java.util.Optional;

public class InfraiClient {
    private final HttpClient httpClient;
    private final String baseUrl;
    private final String apiKey;

    public InfraiClient(AppConfig config) {
        this(HttpClient.newHttpClient(), config.infraiBaseUrl(), config.infraiApiKey());
    }

    public InfraiClient(HttpClient httpClient, String baseUrl, String apiKey) {
        this.httpClient = httpClient;
        this.baseUrl = baseUrl;
        this.apiKey = apiKey;
    }

    public String rtcRoomCreate(String bodyJson) {
        return sendWithRetry("POST", "/v1/rtc/room/create", Optional.of(bodyJson), "application/json");
    }

    public String rtcTokenIssue(String bodyJson) {
        return sendWithRetry("POST", "/v1/rtc/token/issue", Optional.of(bodyJson), "application/json");
    }

    public String storageObjectPut(String bucket, String key, String bodyJson) {
        String encodedBucket = encodePathSegment(bucket);
        String encodedKey = encodePathKeepingSlashes(key);
        return sendWithRetry("PUT", "/v1/storage/object/put/" + encodedBucket + "/" + encodedKey, Optional.of(bodyJson), "application/json");
    }

    private String sendWithRetry(String method, String path, Optional<String> body, String contentType) {
        int attempts = 0;
        while (true) {
            attempts++;
            HttpRequest.Builder builder = HttpRequest.newBuilder()
                    .uri(URI.create(baseUrl + path))
                    .timeout(Duration.ofSeconds(30))
                    .header("Authorization", "Bearer " + apiKey)
                    .header("Accept", "application/json");

            if (body.isPresent()) {
                builder.header("Content-Type", contentType);
            }

            HttpRequest request = builder.method(method, body.map(HttpRequest.BodyPublishers::ofString).orElse(HttpRequest.BodyPublishers.noBody())).build();

            try {
                HttpResponse<String> response = httpClient.send(request, HttpResponse.BodyHandlers.ofString());
                String responseBody = response.body() == null ? "" : response.body();
                Envelope envelope = Envelope.parse(responseBody);
                if (!envelope.ok()) {
                    if (response.statusCode() == 429 && attempts < 4) {
                        sleep(retryDelayMillis(response, attempts));
                        continue;
                    }
                    throw new InfraiException(envelope.errorCode(), envelope.errorMessage(), response.statusCode());
                }
                return responseBody;
            } catch (IOException e) {
                throw new RuntimeException("I/O failure calling Infrai", e);
            } catch (InterruptedException e) {
                Thread.currentThread().interrupt();
                throw new RuntimeException("Interrupted while calling Infrai", e);
            }
        }
    }

    private long retryDelayMillis(HttpResponse<String> response, int attempts) {
        Optional<String> retryAfter = response.headers().firstValue("Retry-After");
        if (retryAfter.isPresent()) {
            try {
                return Long.parseLong(retryAfter.get()) * 1000L;
            } catch (NumberFormatException ignored) {
            }
        }
        return (long) Math.pow(2, attempts - 1) * 200L;
    }

    private void sleep(long millis) throws InterruptedException {
        Thread.sleep(millis);
    }

    private String encodePathSegment(String value) {
        return URLEncoder.encode(value, StandardCharsets.UTF_8);
    }

    private String encodePathKeepingSlashes(String value) {
        return encodePathSegment(value).replace("+", "%20").replace("%2F", "/");
    }
}
