/** One ranked hit returned from a search. */
public class SearchResult {
    private final String documentId;
    private final String title;
    private final int matchCount;
    private final int importance;

    public SearchResult(String documentId, String title, int matchCount, int importance) {
        this.documentId = documentId;
        this.title = title;
        this.matchCount = matchCount;
        this.importance = importance;
    }

    public String getDocumentId() { return documentId; }
    public String getTitle() { return title; }
    public int getMatchCount() { return matchCount; }
    public int getImportance() { return importance; }

    @Override
    public String toString() {
        return String.format("%-12s | matches=%d | importance=%d | %s",
                documentId, matchCount, importance, title);
    }
}
