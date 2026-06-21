package io.disclose.burplookup;

import burp.api.montoya.BurpExtension;
import burp.api.montoya.MontoyaApi;

/**
 * Entry point for the "Disclosure Contact Lookup" Burp Suite extension.
 *
 * <p>Adds a right-click context menu item ("Find disclosure contact") that
 * resolves the selected request's host to its security-disclosure contacts
 * via lookup.disclose.io, and renders the result in a dedicated suite tab.</p>
 */
public class LookupExtension implements BurpExtension {

    public static final String EXTENSION_NAME = "Disclosure Contact Lookup";

    @Override
    public void initialize(MontoyaApi api) {
        api.extension().setName(EXTENSION_NAME);

        // The suite tab that displays results + a running log of lookups.
        ResultsTab resultsTab = new ResultsTab(api);
        api.userInterface().registerSuiteTab("Disclosure Lookup", resultsTab.getComponent());

        // The context menu provider that triggers a lookup on the selected host.
        LookupContextMenuProvider provider = new LookupContextMenuProvider(api, resultsTab);
        api.userInterface().registerContextMenuItemsProvider(provider);

        api.logging().logToOutput(EXTENSION_NAME + " loaded. "
                + "Right-click a request/host and choose \"Find disclosure contact\".");
    }
}
