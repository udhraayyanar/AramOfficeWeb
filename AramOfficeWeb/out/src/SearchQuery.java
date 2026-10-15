import java.util.*;

/** Wraps a raw search request (keywords + optional filters) as normalized data. */
public class SearchQuery {
    private final Set<String> keywords;
    private final String departmentFilter; // nullable
    private final String categoryFilter;   // nullable

    public SearchQuery(Collection<String> rawKeywords, String departmentFilter, String categoryFilter) {
        this.keywords = TextUtils.normalizeKeywordSet(rawKeywords);
        this.departmentFilter = departmentFilter;
        this.categoryFilter = categoryFilter;
    }

    public Set<String> getKeywords() { return keywords; }
    public String getDepartmentFilter() { return departmentFilter; }
    public String getCategoryFilter() { return categoryFilter; }
}
