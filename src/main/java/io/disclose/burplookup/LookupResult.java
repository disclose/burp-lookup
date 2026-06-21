package io.disclose.burplookup;

import com.google.gson.annotations.SerializedName;

import java.util.ArrayList;
import java.util.List;

/**
 * Typed view of the lookup.disclose.io {@code POST /api/lookup} response body.
 *
 * <p>Only the fields this extension renders are mapped; Gson silently ignores
 * the rest ({@code dataSources}, {@code chains}, {@code requestId}, ...).
 * Mirrors the {@code LookupResult} schema in the published OpenAPI spec
 * (version 2.1.0).</p>
 */
public class LookupResult {

    private String input;
    private String assetType;
    private String status; // complete | partial | failed
    private boolean hasErrors;
    private Attribution attribution;
    private List<Contact> contacts;
    private Details details;

    public String input() {
        return input;
    }

    public String assetType() {
        return assetType != null ? assetType : "unknown";
    }

    public String status() {
        return status != null ? status : "unknown";
    }

    public boolean hasErrors() {
        return hasErrors;
    }

    public Attribution attribution() {
        return attribution;
    }

    /** Contacts ranked best-first: verified before unverified, then high→low confidence. */
    public List<Contact> rankedContacts() {
        List<Contact> ranked = new ArrayList<>(contacts != null ? contacts : List.of());
        ranked.sort((a, b) -> {
            int byVerified = Boolean.compare(b.verified(), a.verified());
            if (byVerified != 0) {
                return byVerified;
            }
            return Integer.compare(confidenceRank(b.confidence()), confidenceRank(a.confidence()));
        });
        return ranked;
    }

    /** Human-readable explanation for failed/reserved inputs, if present. */
    public String detailExplanation() {
        if (details == null) {
            return null;
        }
        if (details.explanation != null && !details.explanation.isBlank()) {
            return details.explanation;
        }
        return details.voice;
    }

    static int confidenceRank(String confidence) {
        if (confidence == null) {
            return 0;
        }
        switch (confidence.toLowerCase()) {
            case "high":
                return 3;
            case "medium":
                return 2;
            case "low":
                return 1;
            default:
                return 0;
        }
    }

    /** Attribution sub-object: who owns the asset. */
    public static class Attribution {
        private String organization;
        private String jurisdiction;
        private String industry;
        private String confidence;
        private String parentCompany;

        public String organization() {
            return organization;
        }

        public String jurisdiction() {
            return jurisdiction;
        }

        public String industry() {
            return industry;
        }

        public String confidence() {
            return confidence;
        }

        public String parentCompany() {
            return parentCompany;
        }
    }

    /** A single contact channel for vulnerability disclosure. */
    public static class Contact {
        private String type;
        private String value;
        private String confidence;
        private String source;
        private String label;
        private boolean verified;

        public String type() {
            return type != null ? type : "";
        }

        public String value() {
            return value != null ? value : "";
        }

        public String confidence() {
            return confidence != null ? confidence : "";
        }

        public String source() {
            return source != null ? source : "";
        }

        public String label() {
            return label != null ? label : "";
        }

        public boolean verified() {
            return verified;
        }
    }

    /** Free-form diagnostic detail (populated for reserved/failed inputs). */
    public static class Details {
        @SerializedName("explanation")
        private String explanation;
        @SerializedName("voice")
        private String voice;
        @SerializedName("suggestion")
        private String suggestion;
    }
}
