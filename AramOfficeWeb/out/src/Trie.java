import java.util.*;

/** Stores every normalized keyword/title-token ever seen, for fast prefix suggestions. */
public class Trie {
    private final TrieNode root = new TrieNode();

    public void insert(String word) {
        String w = TextUtils.normalizeToken(word);
        if (w.isEmpty()) return;
        TrieNode cur = root;
        for (char c : w.toCharArray()) {
            cur = cur.children.computeIfAbsent(c, k -> new TrieNode());
        }
        cur.endOfWord = true;
    }

    public List<String> wordsWithPrefix(String prefix, int limit) {
        List<String> results = new ArrayList<>();
        String p = TextUtils.normalizeToken(prefix); // normalization makes this case-insensitive
        TrieNode cur = root;
        for (char c : p.toCharArray()) {
            cur = cur.children.get(c);
            if (cur == null) return results; // prefix not in trie at all
        }
        collect(cur, new StringBuilder(p), results, limit);
        Collections.sort(results);
        return results;
    }

    private void collect(TrieNode node, StringBuilder prefix, List<String> results, int limit) {
        if (results.size() >= limit) return;
        if (node.endOfWord) results.add(prefix.toString());
        List<Character> keys = new ArrayList<>(node.children.keySet());
        Collections.sort(keys);
        for (char c : keys) {
            if (results.size() >= limit) return;
            prefix.append(c);
            collect(node.children.get(c), prefix, results, limit);
            prefix.deleteCharAt(prefix.length() - 1);
        }
    }
}
