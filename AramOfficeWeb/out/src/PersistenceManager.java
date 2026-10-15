import java.io.*;
import java.util.*;

/**
 * Saves the current document store to a simple pipe-delimited text file,
 * and reloads it on the next run - this is the optional "local-file
 * persistence" the project brief allows on top of the in-memory design.
 *
 * File format: one line per version, grouped by document ID.
 *   docType|documentId|department|versionNumber|title|category|importance|keyword1,keyword2,...
 */
public class PersistenceManager {

    /** Writes every document's full version history to disk. */
    public static void save(DocumentSearch search, String filePath) throws IOException {
        try (BufferedWriter bw = new BufferedWriter(new FileWriter(filePath))) {
            for (GovernmentDocument doc : search.getAllDocuments()) {
                for (DocumentVersion v : doc.getVersionHistory()) {
                    String keywords = String.join(",", v.getKeywords());
                    String line = String.join("|",
                            doc.getDocType(),
                            doc.getDocumentId(),
                            doc.getDepartment(),
                            String.valueOf(v.getVersionNumber()),
                            v.getTitle(),
                            v.getCategory(),
                            String.valueOf(v.getImportance()),
                            keywords);
                    bw.write(line);
                    bw.newLine();
                }
            }
        }
    }

    /**
     * Loads documents from disk, if the file exists.
     * Returns true if data was actually loaded, false if there was no file to load
     * (in which case the caller should fall back to the synthetic dataset).
     */
    public static boolean load(DocumentSearch search, String filePath) throws IOException {
        File f = new File(filePath);
        if (!f.exists()) return false;

        // Group lines by document ID, preserving the order versions were written in.
        Map<String, List<String[]>> byId = new LinkedHashMap<>();
        try (BufferedReader br = new BufferedReader(new FileReader(f))) {
            String line;
            while ((line = br.readLine()) != null) {
                if (line.trim().isEmpty()) continue;
                String[] parts = line.split("\\|", -1);
                if (parts.length < 8) continue; // skip malformed lines defensively
                byId.computeIfAbsent(parts[1], k -> new ArrayList<>()).add(parts);
            }
        }
        if (byId.isEmpty()) return false;

        for (List<String[]> versions : byId.values()) {
            versions.sort(Comparator.comparingInt(p -> Integer.parseInt(p[3])));
            String type = versions.get(0)[0];
            String id = versions.get(0)[1];
            String department = versions.get(0)[2];

            GovernmentDocument doc;
            switch (type) {
                case "Circular": doc = new CircularDocument(id, department); break;
                case "ApplicationForm": doc = new ApplicationForm(id, department); break;
                default: doc = new GovernmentDocument(id, department);
            }

            boolean first = true;
            for (String[] p : versions) {
                String title = p[4];
                String category = p[5];
                int importance = Integer.parseInt(p[6]);
                Set<String> keywords = p[7].isEmpty()
                        ? new LinkedHashSet<>()
                        : new LinkedHashSet<>(Arrays.asList(p[7].split(",")));
                DocumentVersion v = new DocumentVersion(Integer.parseInt(p[3]), title,
                        TextUtils.normalizeKeywordSet(keywords), category, importance, "");
                if (first) {
                    search.registerDocument(doc, v);
                    first = false;
                } else {
                    search.addNewVersion(id, v);
                }
            }
        }
        return true;
    }
}
