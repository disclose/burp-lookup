package io.disclose.burplookup;

import com.google.gson.Gson;
import com.google.gson.JsonObject;
import com.sun.net.httpserver.HttpServer;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import java.io.IOException;
import java.net.InetSocketAddress;
import java.nio.charset.StandardCharsets;
import java.util.concurrent.atomic.AtomicInteger;
import java.util.concurrent.atomic.AtomicReference;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

/**
 * Integration tests for {@link LookupClient} against a local JDK HttpServer.
 * The privacy-contract test (BApp Store Criterion 8) is the important one: it
 * proves the client transmits ONLY the host, never any of the user's request.
 */
class LookupClientTest {

    private static final String RECORDED =
            "{\"input\":\"cloudflare.com\",\"assetType\":\"domain\",\"status\":\"complete\","
            + "\"attribution\":{\"organization\":\"Cloudflare\",\"jurisdiction\":\"US\",\"confidence\":\"high\"},"
            + "\"contacts\":[{\"type\":\"bug_bounty\",\"value\":\"https://x/y\",\"confidence\":\"high\",\"verified\":true}]}";

    private HttpServer server;
    private final AtomicReference<String> lastBody = new AtomicReference<>();
    private final AtomicInteger hits = new AtomicInteger();
    private volatile int status = 200;
    private volatile String responseBody = RECORDED;
    private String endpoint;

    @BeforeEach
    void start() throws IOException {
        server = HttpServer.create(new InetSocketAddress("127.0.0.1", 0), 0);
        server.createContext("/api/lookup", exchange -> {
            hits.incrementAndGet();
            lastBody.set(new String(exchange.getRequestBody().readAllBytes(), StandardCharsets.UTF_8));
            byte[] out = responseBody.getBytes(StandardCharsets.UTF_8);
            exchange.sendResponseHeaders(status, out.length);
            exchange.getResponseBody().write(out);
            exchange.close();
        });
        server.start();
        endpoint = "http://127.0.0.1:" + server.getAddress().getPort() + "/api/lookup";
    }

    @AfterEach
    void stop() {
        server.stop(0);
    }

    @Test
    void sendsOnlyTheHostInTheBody() throws Exception {
        new LookupClient(endpoint).lookup("github.com");
        JsonObject sent = new Gson().fromJson(lastBody.get(), JsonObject.class);
        assertEquals(1, sent.keySet().size(), "request body must contain ONLY the input key");
        assertEquals("github.com", sent.get("input").getAsString());
    }

    @Test
    void parsesResponseIntoResult() throws Exception {
        LookupResult r = new LookupClient(endpoint).lookup("cloudflare.com");
        assertEquals("Cloudflare", r.attribution().organization());
        assertEquals(1, r.rankedContacts().size());
    }

    @Test
    void cachesRepeatLookupsOfSameHost() throws Exception {
        LookupClient client = new LookupClient(endpoint);
        client.lookup("cloudflare.com");
        client.lookup("cloudflare.com");
        assertEquals(1, hits.get(), "a repeat lookup of the same host must hit the cache, not the network");
    }

    @Test
    void rateLimitedThrowsLookupException() {
        status = 429;
        LookupException ex = assertThrows(LookupException.class,
                () -> new LookupClient(endpoint).lookup("example.com"));
        assertTrue(ex.getMessage().toLowerCase().contains("rate limited"));
    }

    @Test
    void malformedJsonThrowsLookupException() {
        responseBody = "{\"input\":"; // truncated JSON -> guaranteed parse failure
        assertThrows(LookupException.class, () -> new LookupClient(endpoint).lookup("example.org"));
    }
}
