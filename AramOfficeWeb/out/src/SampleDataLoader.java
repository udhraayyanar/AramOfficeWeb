import java.util.*;

/** Populates the system with a reproducible synthetic dataset (4 departments x 6 categories x 2 docs = 48). */
public class SampleDataLoader {

    private static final String[] DEPARTMENTS = {"Revenue", "PublicWorks", "Health", "Administration"};
    private static final String[] CATEGORIES = {
            "Circular", "ApplicationForm", "ServiceInstruction", "Notification", "Guideline", "Report"
    };

    static final Map<String, String[]> KEYWORD_POOL = new HashMap<>();
    static {
        KEYWORD_POOL.put("Circular", new String[]{"circular", "notice", "policy", "update", "office"});
        KEYWORD_POOL.put("ApplicationForm", new String[]{"form", "application", "request", "citizen", "submit"});
        KEYWORD_POOL.put("ServiceInstruction", new String[]{"service", "instruction", "process", "staff", "manual"});
        KEYWORD_POOL.put("Notification", new String[]{"notification", "alert", "deadline", "schedule", "public"});
        KEYWORD_POOL.put("Guideline", new String[]{"guideline", "standard", "procedure", "compliance", "rule"});
        KEYWORD_POOL.put("Report", new String[]{"report", "summary", "audit", "review", "annual"});
    }

    /** Allowed (related) keywords for a category, or null if the category is unknown. */
    static String[] poolFor(String cat) {
        for (Map.Entry<String, String[]> e : KEYWORD_POOL.entrySet())
            if (e.getKey().equalsIgnoreCase(cat)) return e.getValue();
        return null;
    }

    public static void load(DocumentSearch search) {
        Random rand = new Random(42); // fixed seed => same dataset every run
        int docsPerCombo = 2;
        int serial = 1;

        for (String dept : DEPARTMENTS) {
            String deptCode = dept.substring(0, Math.min(3, dept.length())).toUpperCase();
            for (String cat : CATEGORIES) {
                String catCode = cat.substring(0, Math.min(3, cat.length())).toUpperCase();
                String[] pool = KEYWORD_POOL.get(cat);

                for (int i = 0; i < docsPerCombo; i++) {
                    String id = String.format("%s-%s-%03d", deptCode, catCode, serial++);
                    String title = cat + " document for " + dept + " office #" + (i + 1);
                    int importance = 1 + rand.nextInt(5);

                    Set<String> keywords = new LinkedHashSet<>();
                    keywords.add(pool[rand.nextInt(pool.length)]);   // picks 2 random words
                    keywords.add(pool[rand.nextInt(pool.length)]);
                    keywords.add(dept.toLowerCase());
                    keywords.add(pool[0]);   // category main keyword: circular / form / service / notification / guideline / report

                    DocumentVersion v1 = new DocumentVersion(1, title,
                            TextUtils.normalizeKeywordSet(keywords), cat, importance,
                            "Synthetic body text for " + title);

                    GovernmentDocument doc;
                    switch (cat) {
                        case "Circular": doc = new CircularDocument(id, dept); break;
                        case "ApplicationForm": doc = new ApplicationForm(id, dept); break;
                        default: doc = new GovernmentDocument(id, dept);
                    }
                    search.registerDocument(doc, v1);
                }
            }
        }
    }
}
