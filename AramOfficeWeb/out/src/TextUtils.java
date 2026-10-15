import java.util.*;

/**
 * Central place for keyword normalization so every part of the system
 * (indexing, searching, trie) treats "Form", "form", " form " the same way.
 */
public class TextUtils {
    private TextUtils() {}

    public static String normalizeToken(String s) {
        if (s == null) return "";
        return s.toLowerCase().replaceAll("[^a-z0-9]", "").trim();
    }

    public static Set<String> normalizeKeywordSet(Collection<String> raw) {
        Set<String> out = new LinkedHashSet<>();
        for (String s : raw) {
            String n = normalizeToken(s);
            if (!n.isEmpty()) out.add(n);
        }
        return out;
    }

    /** Breaks a title into normalized tokens so title words are also searchable. */
    public static Set<String> tokenizeTitle(String title) {
        Set<String> out = new LinkedHashSet<>();
        if (title == null) return out;
        for (String w : title.split("[^A-Za-z0-9]+")) {
            String n = normalizeToken(w);
            if (!n.isEmpty()) out.add(n);
        }
        return out;
    }
}
