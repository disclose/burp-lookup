package io.disclose.burplookup;

import burp.api.montoya.MontoyaApi;
import burp.api.montoya.core.ToolType;
import burp.api.montoya.http.message.HttpRequestResponse;
import burp.api.montoya.ui.contextmenu.ContextMenuEvent;
import burp.api.montoya.ui.contextmenu.ContextMenuItemsProvider;

import javax.swing.JMenuItem;
import java.awt.Component;
import java.util.ArrayList;
import java.util.List;

/**
 * Provides the "Find disclosure contact" context menu item.
 *
 * <p>The menu item appears whenever the invocation has an associated HTTP
 * request from which a host can be extracted. Selecting it sends ONLY the
 * host string (never the request body or any sensitive content) to
 * lookup.disclose.io and surfaces the attribution + contacts in the suite tab.</p>
 */
public class LookupContextMenuProvider implements ContextMenuItemsProvider {

    private final MontoyaApi api;
    private final ResultsTab resultsTab;
    private final LookupClient client;

    public LookupContextMenuProvider(MontoyaApi api, ResultsTab resultsTab) {
        this.api = api;
        this.resultsTab = resultsTab;
        this.client = new LookupClient();
    }

    @Override
    public List<Component> provideMenuItems(ContextMenuEvent event) {
        String host = extractHost(event);
        if (host == null) {
            return null;
        }

        JMenuItem item = new JMenuItem("Find disclosure contact (" + host + ")");
        item.addActionListener(e -> performLookup(host));

        List<Component> items = new ArrayList<>();
        items.add(item);
        return items;
    }

    /**
     * Extracts the host from the current context menu invocation, preferring the
     * message editor selection and falling back to the first selected
     * request/response. Returns {@code null} when no host can be determined.
     */
    private String extractHost(ContextMenuEvent event) {
        HttpRequestResponse requestResponse = null;

        if (event.messageEditorRequestResponse().isPresent()) {
            requestResponse = event.messageEditorRequestResponse().get().requestResponse();
        } else if (!event.selectedRequestResponses().isEmpty()) {
            requestResponse = event.selectedRequestResponses().get(0);
        }

        if (requestResponse == null || requestResponse.request() == null) {
            return null;
        }

        try {
            String host = requestResponse.request().httpService().host();
            if (host == null || host.isBlank()) {
                return null;
            }
            return host.trim();
        } catch (Exception ex) {
            return null;
        }
    }

    /**
     * Runs the lookup off the Swing Event Dispatch Thread so the UI never freezes,
     * then marshals the result back onto the EDT for rendering.
     */
    private void performLookup(String host) {
        resultsTab.showPending(host);
        api.logging().logToOutput("Looking up disclosure contact for host: " + host);

        // Background thread: the blocking HTTP call must not run on the EDT.
        Thread worker = new Thread(() -> {
            try {
                LookupResult result = client.lookup(host);
                resultsTab.showResult(host, result);
                api.logging().logToOutput("Lookup complete for " + host
                        + " (status=" + result.status() + ").");
            } catch (LookupException ex) {
                resultsTab.showError(host, ex.getMessage());
                api.logging().logToError("Lookup failed for " + host + ": " + ex.getMessage());
            }
        }, "disclose-lookup-" + host);
        worker.setDaemon(true);
        worker.start();
    }
}
