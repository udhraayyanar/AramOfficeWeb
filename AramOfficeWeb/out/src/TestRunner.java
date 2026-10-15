import java.util.*;

/**
 * Documented test cases:
 *  1. No keyword match   2. Mixed-case prefix search   3. New version replaces index terms
 *  4. Duplicate document ID rejected   5. Document IDs in ascending / descending order
 */
public class TestRunner {

    /** Number at the end of an ID (REV-CIR-007 -> 7); IDs without a number go last. */
    static int idNumber(String id) {
        try { return Integer.parseInt(id.substring(id.lastIndexOf('-') + 1)); }
        catch (Exception e) { return Integer.MAX_VALUE; }
    }

    /** ID order: by number, then alphabetically if numbers are equal (always deterministic). */
    static final Comparator<String> BY_ID_NUMBER =
            Comparator.comparingInt(TestRunner::idNumber).thenComparing(Comparator.naturalOrder());

    static void printIds(String label, List<String> ids) {
        System.out.println(label + ":");
        for (int i = 0; i < ids.size(); i += 6)
            System.out.println("  " + String.join(", ", ids.subList(i, Math.min(i + 6, ids.size()))));
    }

    public static void run(DocumentSearch search) {
        System.out.println("=== TEST 1: No keyword match ===");
        List<SearchResult> r1 = search.search(new SearchQuery(List.of("zzznomatch"), null, null));
        System.out.println("Expected: 0 results. Actual: " + r1.size());

        System.out.println("\n=== TEST 2: Mixed-case prefix search ===");
        List<String> r2 = search.prefixSuggestions("ReP", 10); // "ReP" should behave exactly like "rep"
        System.out.println("Prefix 'ReP' suggestions: " + r2);

        System.out.println("\n=== TEST 3: New version replaces index terms ===");
        GovernmentDocument sample = search.getAllDocuments().iterator().next();
        String id = sample.getDocumentId();
        Set<String> oldKeywords = new LinkedHashSet<>(sample.getCurrentVersion().getKeywords());
        System.out.println("Using document: " + id + " (current keywords: " + oldKeywords + ")");

        DocumentVersion v2 = new DocumentVersion(
                sample.getCurrentVersion().getVersionNumber() + 1,
                sample.getCurrentVersion().getTitle() + " (revised)",
                TextUtils.normalizeKeywordSet(List.of("brandnewterm", "revised")),
                sample.getCurrentVersion().getCategory(),
                sample.getCurrentVersion().getImportance(),
                "Revised content");
        search.addNewVersion(id, v2);

        System.out.println("Document now has " + sample.getVersionHistory().size()
                + " versions; current is v" + sample.getCurrentVersion().getVersionNumber());
        List<SearchResult> onNewTerm = search.search(new SearchQuery(List.of("brandnewterm"), null, null));
        boolean foundOnNew = onNewTerm.stream().anyMatch(r -> r.getDocumentId().equals(id));
        System.out.println("Search on NEW term 'brandnewterm' finds updated doc? " + foundOnNew);

        // old keywords must no longer find this document (title words are indexed too, so skip those)
        Set<String> titleWords = TextUtils.tokenizeTitle(v2.getTitle());
        boolean oldGone = true;
        for (String k : oldKeywords) {
            if (titleWords.contains(k)) continue;
            boolean hit = search.search(new SearchQuery(List.of(k), null, null))
                    .stream().anyMatch(r -> r.getDocumentId().equals(id));
            if (hit) oldGone = false;
        }
        System.out.println("Old keywords no longer find the document? " + oldGone);

        System.out.println("\n=== TEST 4: Duplicate document ID rejected ===");
        try {
            GovernmentDocument dupe = new GovernmentDocument(id, sample.getDepartment());
            DocumentVersion dv = new DocumentVersion(1, "Duplicate attempt",
                    TextUtils.normalizeKeywordSet(List.of("dup")), "Report", 1, "text");
            search.registerDocument(dupe, dv);
            System.out.println("FAIL: duplicate was not rejected");
        } catch (DuplicateDocumentException e) {
            System.out.println("PASS: duplicate rejected with message: " + e.getMessage());
        }

        System.out.println("\n=== TEST 5: Document IDs in ascending and descending order ===");
        List<String> ids = new ArrayList<>();
        for (GovernmentDocument d : search.getAllDocuments()) ids.add(d.getDocumentId());
        System.out.println("Total documents: " + ids.size()
                + " | All IDs unique? " + (new HashSet<>(ids).size() == ids.size()));
        List<String> asc = new ArrayList<>(ids);
        asc.sort(BY_ID_NUMBER);
        List<String> desc = new ArrayList<>(ids);
        desc.sort(BY_ID_NUMBER.reversed());
        printIds("Ascending", asc);
        printIds("Descending", desc);
        List<String> check = new ArrayList<>(asc);
        Collections.reverse(check);
        System.out.println(desc.equals(check)
                ? "PASS: descending is the exact reverse of ascending"
                : "FAIL: order mismatch");
    }
}
