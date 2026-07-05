package io.disclose.burplookup;

import com.google.gson.Gson;
import com.google.gson.JsonSyntaxException;

import java.net.URI;
import java.net.http.HttpClient;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;
import java.time.Duration;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

/**
 * Thin client for the lookup.disclose.io attribution API.
 *
 * <p>Privacy contract (BApp Store Criterion 8): this client sends ONLY the host
 * string in the request body — never the request line, headers, cookies, or
 * body of the user's selected HTTP message.</p>
 *
 * <p>A short-lived in-memory cache prevents lookup-spam when the same host is
 * resolved repeatedly. The HTTP call carries a hard timeout so a slow or
 * offline API degrades gracefully instead of hanging.</p>
 */
public class LookupClient {

    private static final String ENDPOINT = "https://lookup.disclose.io/api/lookup";
    private static final Duration CONNECT_TIMEOUT = Duration.ofSeconds(8);
    private static final Duration REQUEST_TIMEOUT = Duration.ofSeconds(15);
    private static final long CACHE_TTL_MILLIS = 5 * 60 * 1000L; // 5 minutes

    private final HttpClient httpClient;
    private final Gson gson;
    private final String endpoint;
    private final Map<String, CacheEntry> cache = new ConcurrentHashMap<>();

    public LookupClient() {
        this(ENDPOINT);
    }

    /**
     * Package-private constructor used by the test suite to point the client at a
     * local mock server. Production code uses the no-arg constructor, which pins
     * the endpoint to {@link #ENDPOINT}.
     */
    LookupClient(String endpoint) {
        this.endpoint = endpoint;
        this.httpClient = HttpClient.newBuilder()
                .connectTimeout(CONNECT_TIMEOUT)
                .followRedirects(HttpClient.Redirect.NORMAL)
                .build();
        this.gson = new Gson();
    }

    /**
     * Resolves a host to its attribution + disclosure contacts.
     *
     * @param host the bare hostname (e.g. {@code github.com}) — nothing else is sent
     * @return the parsed result
     * @throws LookupException on network error, non-2xx response, or malformed JSON
     */
    public LookupResult lookup(String host) throws LookupException {
        String key = host.toLowerCase();

        CacheEntry cached = cache.get(key);
        if (cached != null && !cached.isExpired()) {
            return cached.result;
        }

        // Only the host travels in the body. Build via Gson to guarantee valid JSON.
        String body = gson.toJson(Map.of("input", host));

        HttpRequest request = HttpRequest.newBuilder()
                .uri(URI.create(endpoint))
                .timeout(REQUEST_TIMEOUT)
                .header("Content-Type", "application/json")
                .header("Accept", "application/json")
                .header("User-Agent", "burp-lookup/1.0 (+https://github.com/disclose/burp-lookup)")
                .POST(HttpRequest.BodyPublishers.ofString(body))
                .build();

        HttpResponse<String> response;
        try {
            response = httpClient.send(request, HttpResponse.BodyHandlers.ofString());
        } catch (java.net.http.HttpTimeoutException e) {
            throw new LookupException("Lookup timed out after " + REQUEST_TIMEOUT.toSeconds()
                    + "s. The service may be slow or unreachable.", e);
        } catch (java.io.IOException e) {
            throw new LookupException("Could not reach lookup.disclose.io (offline?): "
                    + e.getMessage(), e);
        } catch (InterruptedException e) {
            Thread.currentThread().interrupt();
            throw new LookupException("Lookup was interrupted.", e);
        }

        int code = response.statusCode();
        if (code == 429) {
            throw new LookupException("Rate limited by lookup.disclose.io (30 req/min). "
                    + "Please wait a moment and try again.");
        }
        if (code < 200 || code >= 300) {
            throw new LookupException("lookup.disclose.io returned HTTP " + code + ".");
        }

        LookupResult result;
        try {
            result = gson.fromJson(response.body(), LookupResult.class);
        } catch (JsonSyntaxException e) {
            throw new LookupException("Could not parse the lookup response.", e);
        }
        if (result == null) {
            throw new LookupException("Empty response from lookup.disclose.io.");
        }

        cache.put(key, new CacheEntry(result));
        return result;
    }

    private static final class CacheEntry {
        final LookupResult result;
        final long storedAt;

        CacheEntry(LookupResult result) {
            this.result = result;
            this.storedAt = System.currentTimeMillis();
        }

        boolean isExpired() {
            return System.currentTimeMillis() - storedAt > CACHE_TTL_MILLIS;
        }
    }
}
