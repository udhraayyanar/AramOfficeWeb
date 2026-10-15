import java.util.*;

/**
 * Ranks search hits. Tie-break order is fully deterministic:
 * higher keyword-match count wins, then higher importance, then lower
 * document ID alphabetically - so re-running the same query always
 * produces the same order.
 */
public class RankingEngine {

    private static final Comparator<SearchResult> BEST_FIRST =
            Comparator.comparingInt(SearchResult::getMatchCount).reversed()
                    .thenComparing(Comparator.comparingInt(SearchResult::getImportance).reversed())
                    .thenComparing(SearchResult::getDocumentId);

    /** Full sort - use when every matching document should be returned. */
    public List<SearchResult> rankAll(List<SearchResult> candidates) {
        List<SearchResult> copy = new ArrayList<>(candidates);
        copy.sort(BEST_FIRST);
        return copy;
    }

    /**
     * Bounded PriorityQueue approach - keeps only the top K in a heap of
     * size K instead of sorting everything. Useful when the candidate set
     * could be large and only the best few results matter.
     */
    public List<SearchResult> topK(List<SearchResult> candidates, int k) {
        Comparator<SearchResult> worstFirst = BEST_FIRST.reversed();
        PriorityQueue<SearchResult> heap = new PriorityQueue<>(worstFirst);

        for (SearchResult r : candidates) {
            heap.offer(r);
            if (heap.size() > k) {
                heap.poll(); // evict the current weakest entry, keeping the heap bounded to size k
            }
        }
        List<SearchResult> result = new ArrayList<>(heap);
        result.sort(BEST_FIRST);
        return result;
    }
}
