import java.util.*;

/**
 * Ties everything together: document storage, the inverted index,
 * the prefix trie, and the category tree. This is the "abstraction"
 * layer the rest of the app (Main, TestRunner) talks to.
 */
public class DocumentSearch {
    private final Map<String, GovernmentDocument> documentsById = new HashMap<>();
    private final Map<String, Set<String>> invertedIndex = new HashMap<>(); // keyword -> doc IDs (current version only)
    private final Trie trie = new Trie();
    private final CategoryTree categoryTree = new CategoryTree();
    private final RankingEngine rankingEngine = new RankingEngine();

    public CategoryTree getCategoryTree() { return categoryTree; }
    public int getDocumentCount() { return documentsById.size(); }
    public Collection<GovernmentDocument> getAllDocuments() { return documentsById.values(); }
    public GovernmentDocument getDocument(String id) { return documentsById.get(id); }

    /** Register a brand-new document together with its first version. */
    public void registerDocument(GovernmentDocument doc, DocumentVersion firstVersion) {
        if (documentsById.containsKey(doc.getDocumentId())) {
            throw new DuplicateDocumentException(doc.getDocumentId());
        }
        doc.addVersion(firstVersion);
        doc.setCurrentVersion(firstVersion.getVersionNumber());
        documentsById.put(doc.getDocumentId(), doc);
        indexCurrentVersion(doc);
        categoryTree.getOrCreatePath(doc.getDepartment(), firstVersion.getCategory())
                .attachDocument(doc.getDocumentId());
    }

    /** Add a new version to an existing document and make it current, refreshing the index. */
    public void addNewVersion(String documentId, DocumentVersion newVersion) {
        GovernmentDocument doc = documentsById.get(documentId);
        if (doc == null) throw new NoSuchElementException("No such document: " + documentId);

        DocumentVersion oldCurrent = doc.getCurrentVersion();
        doc.addVersion(newVersion);              // validated + appended to immutable history
        doc.setCurrentVersion(newVersion.getVersionNumber());

        if (oldCurrent != null) removeFromIndex(doc.getDocumentId(), oldCurrent);
        indexCurrentVersion(doc);

        if (oldCurrent == null || !oldCurrent.getCategory().equalsIgnoreCase(newVersion.getCategory())) {
            categoryTree.getOrCreatePath(doc.getDepartment(), newVersion.getCategory())
                    .attachDocument(doc.getDocumentId());
        }
    }

    private void indexCurrentVersion(GovernmentDocument doc) {
        DocumentVersion v = doc.getCurrentVersion();
        Set<String> terms = new HashSet<>(v.getKeywords());
        terms.addAll(TextUtils.tokenizeTitle(v.getTitle()));
        for (String term : terms) {
            invertedIndex.computeIfAbsent(term, k -> new HashSet<>()).add(doc.getDocumentId());
            trie.insert(term);
        }
    }

    private void removeFromIndex(String documentId, DocumentVersion oldVersion) {
        Set<String> terms = new HashSet<>(oldVersion.getKeywords());
        terms.addAll(TextUtils.tokenizeTitle(oldVersion.getTitle()));
        for (String term : terms) {
            Set<String> ids = invertedIndex.get(term);
            if (ids != null) {
                ids.remove(documentId);
                if (ids.isEmpty()) invertedIndex.remove(term);
            }
        }
        // Trie entries are left in place on purpose: they represent known
        // vocabulary for prefix suggestions, not just "currently indexed" terms.
    }

    /** Case-insensitive exact-keyword search over current versions only, fully ranked. */
    public List<SearchResult> search(SearchQuery query) {
        List<SearchResult> candidates = buildCandidates(query);
        return rankingEngine.rankAll(candidates);
    }

    /** Same search, but only the best k results, computed via the bounded-heap ranker. */
    public List<SearchResult> searchTopK(SearchQuery query, int k) {
        List<SearchResult> candidates = buildCandidates(query);
        return rankingEngine.topK(candidates, k);
    }

    private List<SearchResult> buildCandidates(SearchQuery query) {
        Map<String, Integer> matchCount = new HashMap<>();
        for (String kw : query.getKeywords()) {
            Set<String> ids = invertedIndex.get(kw);
            if (ids == null) continue;
            for (String id : ids) {
                GovernmentDocument doc = documentsById.get(id);
                if (query.getDepartmentFilter() != null &&
                        !doc.getDepartment().equalsIgnoreCase(query.getDepartmentFilter())) continue;
                if (query.getCategoryFilter() != null &&
                        !doc.getCurrentVersion().getCategory().equalsIgnoreCase(query.getCategoryFilter())) continue;
                matchCount.merge(id, 1, Integer::sum);
            }
        }
        List<SearchResult> candidates = new ArrayList<>();
        for (Map.Entry<String, Integer> e : matchCount.entrySet()) {
            GovernmentDocument doc = documentsById.get(e.getKey());
            candidates.add(new SearchResult(doc.getDocumentId(), doc.getCurrentVersion().getTitle(),
                    e.getValue(), doc.getCurrentVersion().getImportance()));
        }
        return candidates;
    }

    public List<String> prefixSuggestions(String prefix, int limit) {
        return trie.wordsWithPrefix(prefix, limit);
    }

    public List<GovernmentDocument> byDepartmentAndCategory(String department, String category) {
        CategoryNode node = categoryTree.findPath(department, category);
        List<GovernmentDocument> out = new ArrayList<>();
        if (node == null) return out;
        for (String id : node.getDocumentIds()) out.add(documentsById.get(id));
        return out;
    }
}
