package io.disclose.burplookup;

import burp.api.montoya.MontoyaApi;

import javax.swing.BorderFactory;
import javax.swing.BoxLayout;
import javax.swing.JLabel;
import javax.swing.JPanel;
import javax.swing.JScrollPane;
import javax.swing.JSplitPane;
import javax.swing.JTable;
import javax.swing.JTextArea;
import javax.swing.SwingUtilities;
import javax.swing.table.DefaultTableModel;
import java.awt.BorderLayout;
import java.awt.Component;
import java.awt.Font;
import java.time.LocalTime;
import java.time.format.DateTimeFormatter;
import java.util.List;

/**
 * The suite tab that displays the most recent lookup (attribution header +
 * ranked contacts table) and an append-only activity log.
 *
 * <p>All public methods are safe to call from any thread; every Swing mutation
 * is marshalled onto the Event Dispatch Thread.</p>
 */
public class ResultsTab {

    private static final DateTimeFormatter TIME_FMT = DateTimeFormatter.ofPattern("HH:mm:ss");
    private static final String[] COLUMNS = {"Verified", "Type", "Confidence", "Contact", "Label", "Source"};

    private final MontoyaApi api;

    private final JPanel root;
    private final JLabel attributionLabel;
    private final DefaultTableModel tableModel;
    private final JTextArea logArea;

    public ResultsTab(MontoyaApi api) {
        this.api = api;

        attributionLabel = new JLabel(" Right-click a request and choose \"Find disclosure contact\" to begin.");
        attributionLabel.setBorder(BorderFactory.createEmptyBorder(8, 8, 8, 8));
        Font base = attributionLabel.getFont();
        attributionLabel.setFont(base.deriveFont(Font.PLAIN, base.getSize() + 1f));

        tableModel = new DefaultTableModel(COLUMNS, 0) {
            @Override
            public boolean isCellEditable(int row, int column) {
                return false; // read-only results table
            }
        };
        JTable table = new JTable(tableModel);
        table.setFillsViewportHeight(true);
        table.setAutoResizeMode(JTable.AUTO_RESIZE_LAST_COLUMN);

        JPanel resultsPanel = new JPanel(new BorderLayout());
        resultsPanel.add(attributionLabel, BorderLayout.NORTH);
        resultsPanel.add(new JScrollPane(table), BorderLayout.CENTER);

        logArea = new JTextArea();
        logArea.setEditable(false);
        logArea.setFont(new Font(Font.MONOSPACED, Font.PLAIN, 12));
        JPanel logPanel = new JPanel(new BorderLayout());
        JLabel logHeader = new JLabel(" Activity log");
        logHeader.setBorder(BorderFactory.createEmptyBorder(4, 4, 4, 4));
        logPanel.add(logHeader, BorderLayout.NORTH);
        logPanel.add(new JScrollPane(logArea), BorderLayout.CENTER);

        JSplitPane split = new JSplitPane(JSplitPane.VERTICAL_SPLIT, resultsPanel, logPanel);
        split.setResizeWeight(0.7);
        split.setBorder(null);

        JPanel header = new JPanel();
        header.setLayout(new BoxLayout(header, BoxLayout.Y_AXIS));
        JLabel title = new JLabel(" Disclosure Contact Lookup — powered by lookup.disclose.io");
        title.setBorder(BorderFactory.createEmptyBorder(8, 8, 0, 8));
        title.setFont(title.getFont().deriveFont(Font.BOLD));
        header.add(title);

        root = new JPanel(new BorderLayout());
        root.add(header, BorderLayout.NORTH);
        root.add(split, BorderLayout.CENTER);
    }

    public Component getComponent() {
        return root;
    }

    /** Called when a lookup is dispatched, before the network call returns. */
    public void showPending(String host) {
        SwingUtilities.invokeLater(() -> {
            attributionLabel.setText(" Looking up " + host + " …");
            tableModel.setRowCount(0);
            appendLog("Looking up " + host + " …");
        });
    }

    /** Called with a successful result. */
    public void showResult(String host, LookupResult result) {
        SwingUtilities.invokeLater(() -> {
            attributionLabel.setText(buildAttributionHtml(host, result));
            tableModel.setRowCount(0);

            List<LookupResult.Contact> contacts = result.rankedContacts();
            for (LookupResult.Contact c : contacts) {
                tableModel.addRow(new Object[]{
                        c.verified() ? "yes" : "no",
                        c.type(),
                        c.confidence(),
                        c.value(),
                        c.label(),
                        c.source()
                });
            }

            appendLog(String.format("%s -> status=%s, %d contact(s)%s",
                    host, result.status(), contacts.size(),
                    result.hasErrors() ? " (results may be incomplete)" : ""));
        });
    }

    /** Called on lookup failure (offline, timeout, non-2xx, parse error). */
    public void showError(String host, String message) {
        SwingUtilities.invokeLater(() -> {
            attributionLabel.setText(" Lookup failed for " + host + ": " + message);
            tableModel.setRowCount(0);
            appendLog("ERROR " + host + ": " + message);
        });
    }

    private String buildAttributionHtml(String host, LookupResult result) {
        StringBuilder sb = new StringBuilder("<html><body style='padding:4px'>");
        sb.append("<b>").append(escape(host)).append("</b>");
        sb.append(" &middot; ").append(escape(result.assetType()));
        sb.append(" &middot; status: ").append(escape(result.status()));
        if (result.hasErrors()) {
            sb.append(" &middot; <i>results may be incomplete</i>");
        }
        sb.append("<br>");

        LookupResult.Attribution attr = result.attribution();
        if (attr != null && attr.organization() != null) {
            sb.append("Owner: <b>").append(escape(attr.organization())).append("</b>");
            if (attr.parentCompany() != null) {
                sb.append(" (parent: ").append(escape(attr.parentCompany())).append(")");
            }
            if (attr.jurisdiction() != null) {
                sb.append(" &middot; ").append(escape(attr.jurisdiction()));
            }
            if (attr.confidence() != null) {
                sb.append(" &middot; confidence: ").append(escape(attr.confidence()));
            }
        } else {
            sb.append("No organization attributed.");
            String explanation = result.detailExplanation();
            if (explanation != null) {
                sb.append(" ").append(escape(explanation));
            }
        }
        sb.append("</body></html>");
        return sb.toString();
    }

    private void appendLog(String line) {
        logArea.append("[" + LocalTime.now().format(TIME_FMT) + "] " + line + "\n");
        logArea.setCaretPosition(logArea.getDocument().getLength());
        // Mirror to Burp's own output log for power users.
        if (api != null) {
            api.logging().logToOutput(line);
        }
    }

    private static String escape(String s) {
        if (s == null) {
            return "";
        }
        return s.replace("&", "&amp;").replace("<", "&lt;").replace(">", "&gt;");
    }
}
