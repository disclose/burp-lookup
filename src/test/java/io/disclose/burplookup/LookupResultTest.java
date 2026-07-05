package io.disclose.burplookup;

import com.google.gson.Gson;
import org.junit.jupiter.api.Test;

import java.util.List;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertTrue;

/**
 * Unit tests for the Gson-mapped {@link LookupResult} view of the
 * lookup.disclose.io response. RECORDED is a response captured verbatim from the
 * live API on 2026-07-05 — it pins the contract the extension renders against.
 */
class LookupResultTest {

    private static final Gson GSON = new Gson();

    private static final String RECORDED = """
        {"input":"cloudflare.com","assetType":"domain","status":"complete","hasErrors":false,
         "attribution":{"confidence":"high","organization":"Cloudflare","jurisdiction":"US"},
         "contacts":[
           {"type":"bug_bounty","value":"https://www.cloudflare.com/disclosure/","confidence":"high","verified":true,"label":"Cloudflare (Bounty)"},
           {"type":"convention","value":"security@cloudflare.com","confidence":"low","verified":false}
         ]}
        """;

    @Test
    void parsesRecordedResponse() {
        LookupResult r = GSON.fromJson(RECORDED, LookupResult.class);
        assertEquals("cloudflare.com", r.input());
        assertEquals("domain", r.assetType());
        assertEquals("complete", r.status());
        assertFalse(r.hasErrors());
        assertNotNull(r.attribution());
        assertEquals("Cloudflare", r.attribution().organization());
        assertEquals("US", r.attribution().jurisdiction());
        assertEquals(2, r.rankedContacts().size());
    }

    @Test
    void rankedContactsPutVerifiedHighConfidenceFirst() {
        LookupResult r = GSON.fromJson(RECORDED, LookupResult.class);
        List<LookupResult.Contact> ranked = r.rankedContacts();
        assertTrue(ranked.get(0).verified());
        assertEquals("bug_bounty", ranked.get(0).type());
        assertFalse(ranked.get(1).verified());
    }

    @Test
    void confidenceRankOrdersHighMediumLow() {
        assertEquals(3, LookupResult.confidenceRank("high"));
        assertEquals(2, LookupResult.confidenceRank("MEDIUM"));
        assertEquals(1, LookupResult.confidenceRank("low"));
        assertEquals(0, LookupResult.confidenceRank(null));
        assertEquals(0, LookupResult.confidenceRank("bogus"));
    }

    @Test
    void nullSafeGettersOnEmptyBody() {
        LookupResult r = GSON.fromJson("{}", LookupResult.class);
        assertEquals("unknown", r.assetType());
        assertEquals("unknown", r.status());
        assertTrue(r.rankedContacts().isEmpty());
        assertNull(r.detailExplanation());
    }

    @Test
    void contactGettersNeverReturnNull() {
        LookupResult r = GSON.fromJson("{\"contacts\":[{}]}", LookupResult.class);
        LookupResult.Contact c = r.rankedContacts().get(0);
        assertEquals("", c.type());
        assertEquals("", c.value());
        assertEquals("", c.confidence());
        assertFalse(c.verified());
    }

    @Test
    void detailExplanationFallsBackToVoice() {
        LookupResult r = GSON.fromJson(
                "{\"details\":{\"voice\":\"reserved range\"}}", LookupResult.class);
        assertEquals("reserved range", r.detailExplanation());
    }
}
