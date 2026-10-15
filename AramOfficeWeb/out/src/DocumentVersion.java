import java.time.LocalDateTime;
import java.util.Collections;
import java.util.LinkedHashSet;
import java.util.Set;

/**
 * An immutable snapshot of a document at one point in time.
 * Once created, none of its fields can change - this is what lets us keep
 * "previous versions immutable" while only the *current* pointer moves.
 */
public final class DocumentVersion {
    private final int versionNumber;
    private final String title;
    private final Set<String> keywords;     // already normalized, lowercase
    private final String category;
    private final int importance;           // 1 (low) .. 5 (high)
    private final String contentText;
    private final LocalDateTime createdAt;

    public DocumentVersion(int versionNumber, String title, Set<String> keywords,
                            String category, int importance, String contentText) {
        this.versionNumber = versionNumber;
        this.title = title;
        this.keywords = Collections.unmodifiableSet(new LinkedHashSet<>(keywords));
        this.category = category;
        this.importance = importance;
        this.contentText = contentText;
        this.createdAt = LocalDateTime.now();
    }

    public int getVersionNumber() { return versionNumber; }
    public String getTitle() { return title; }
    public Set<String> getKeywords() { return keywords; }
    public String getCategory() { return category; }
    public int getImportance() { return importance; }
    public String getContentText() { return contentText; }
    public LocalDateTime getCreatedAt() { return createdAt; }

    @Override
    public String toString() {
        return "v" + versionNumber + " | " + title + " | category=" + category
                + " | importance=" + importance + " | keywords=" + keywords;
    }
}
