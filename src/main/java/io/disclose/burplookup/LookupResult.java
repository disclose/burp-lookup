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
    private List<ContactGroup> contactGroups;
    private RouteSummary routeSummary;
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

    /** Flat backward-compatible contacts, preserving the server's authoritative order. */
    public List<Contact> contactsInServerOrder() {
        return new ArrayList<>(contacts != null ? contacts : List.of());
    }

    /**
     * Backward-compatible alias. The API already ranks by route quality; clients
     * must not re-sort and accidentally promote a coordinator above an owner.
     */
    public List<Contact> rankedContacts() {
        return contactsInServerOrder();
    }

    /** Owner-aware route groups, preserving group and contact order from the API. */
    public List<ContactGroup> contactGroups() {
        return new ArrayList<>(contactGroups != null ? contactGroups : List.of());
    }

    public RouteSummary routeSummary() {
        return routeSummary;
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
        private String entity;
        private String entityKey;
        private String relation;
        private String routeClass;
        private String deliveryAgent;
        private boolean authoritative;

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

        public String entity() {
            return entity != null ? entity : "";
        }

        public String relation() {
            return relation != null ? relation : "";
        }

        public String routeClass() {
            return routeClass != null ? routeClass : "";
        }

        public String deliveryAgent() {
            return deliveryAgent != null ? deliveryAgent : "";
        }

        public boolean authoritative() {
            return authoritative;
        }
    }

    /** A server-ranked set of contacts that all reach the same responsible party. */
    public static class ContactGroup {
        private String entity;
        private String entityKey;
        private String relation;
        private String routeClass;
        private String scopeNote;
        private String rationale;
        private List<Contact> contacts;

        public String entity() {
            return entity != null ? entity : "";
        }

        public String relation() {
            return relation != null ? relation : "";
        }

        public String routeClass() {
            return routeClass != null ? routeClass : "";
        }

        public String scopeNote() {
            return scopeNote != null ? scopeNote : "";
        }

        public String rationale() {
            return rationale != null ? rationale : "";
        }

        public List<Contact> contacts() {
            return new ArrayList<>(contacts != null ? contacts : List.of());
        }
    }

    /** Concise explanation of the best reporting route in the response. */
    public static class RouteSummary {
        private String routeClass;
        private String headline;
        private boolean firstPartyFound;
        private boolean ownerRouteFound;
        private boolean coordinatorAvailable;

        public String routeClass() {
            return routeClass != null ? routeClass : "";
        }

        public String headline() {
            return headline != null ? headline : "";
        }

        public boolean firstPartyFound() {
            return firstPartyFound;
        }

        public boolean ownerRouteFound() {
            return ownerRouteFound;
        }

        public boolean coordinatorAvailable() {
            return coordinatorAvailable;
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
