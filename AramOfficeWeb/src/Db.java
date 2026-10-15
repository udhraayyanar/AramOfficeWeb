import java.security.MessageDigest;
import java.sql.*;
import java.util.*;

/** SQLite database layer: staff accounts + documents + versions. */
public class Db {
    static Connection conn() throws SQLException { return DriverManager.getConnection("jdbc:sqlite:aramoffice.db"); }

    static void init() throws Exception {
        try (Connection c = conn(); Statement s = c.createStatement()) {
            s.execute("CREATE TABLE IF NOT EXISTS users(username TEXT PRIMARY KEY, salt TEXT, hash TEXT, full_name TEXT)");
            s.execute("CREATE TABLE IF NOT EXISTS documents(id TEXT PRIMARY KEY, department TEXT, doc_type TEXT)");
            s.execute("CREATE TABLE IF NOT EXISTS versions(doc_id TEXT, version_no INTEGER, title TEXT, category TEXT, importance INTEGER, keywords TEXT, PRIMARY KEY(doc_id, version_no))");
        }
        if (count("users") == 0) {
            addUser("admin", "admin123", "Office Admin");
            addUser("staff", "staff123", "Office Staff");
        }
    }

    static int count(String table) throws SQLException {
        try (Connection c = conn(); Statement s = c.createStatement(); ResultSet r = s.executeQuery("SELECT COUNT(*) FROM " + table)) {
            return r.getInt(1);
        }
    }

    static String hash(String salt, String pw) throws Exception {
        return HexFormat.of().formatHex(MessageDigest.getInstance("SHA-256").digest((salt + pw).getBytes("UTF-8")));
    }

    static void addUser(String user, String pw, String fullName) throws Exception {
        String salt = UUID.randomUUID().toString();
        try (Connection c = conn(); PreparedStatement p = c.prepareStatement("INSERT INTO users VALUES(?,?,?,?)")) {
            p.setString(1, user); p.setString(2, salt); p.setString(3, hash(salt, pw)); p.setString(4, fullName);
            p.executeUpdate();
        }
    }

    /** Returns the staff member's full name if the login is valid, otherwise null. */
    static String login(String user, String pw) throws Exception {
        try (Connection c = conn(); PreparedStatement p = c.prepareStatement("SELECT salt, hash, full_name FROM users WHERE username=?")) {
            p.setString(1, user);
            try (ResultSet r = p.executeQuery()) {
                if (r.next() && r.getString(2).equals(hash(r.getString(1), pw))) return r.getString(3);
            }
        }
        return null;
    }

    static void saveDocument(GovernmentDocument d) throws SQLException {
        try (Connection c = conn(); PreparedStatement p = c.prepareStatement("INSERT OR IGNORE INTO documents VALUES(?,?,?)")) {
            p.setString(1, d.getDocumentId()); p.setString(2, d.getDepartment()); p.setString(3, d.getDocType());
            p.executeUpdate();
        }
        for (DocumentVersion v : d.getVersionHistory()) saveVersion(d.getDocumentId(), v);
    }

    static void saveVersion(String id, DocumentVersion v) throws SQLException {
        try (Connection c = conn(); PreparedStatement p = c.prepareStatement("INSERT OR IGNORE INTO versions VALUES(?,?,?,?,?,?)")) {
            p.setString(1, id); p.setInt(2, v.getVersionNumber()); p.setString(3, v.getTitle());
            p.setString(4, v.getCategory()); p.setInt(5, v.getImportance()); p.setString(6, String.join(",", v.getKeywords()));
            p.executeUpdate();
        }
    }

    /** Rebuilds the in-memory structures (index, trie, tree) from the database at startup. */
    static void loadAll(DocumentSearch search) throws SQLException {
        try (Connection c = conn(); Statement s = c.createStatement();
             ResultSet docs = s.executeQuery("SELECT id, department, doc_type FROM documents ORDER BY id")) {
            while (docs.next()) {
                String id = docs.getString(1), dept = docs.getString(2), type = docs.getString(3);
                GovernmentDocument doc = type.equals("Circular") ? new CircularDocument(id, dept)
                        : type.equals("ApplicationForm") ? new ApplicationForm(id, dept) : new GovernmentDocument(id, dept);
                boolean first = true;
                try (PreparedStatement p = c.prepareStatement("SELECT version_no, title, category, importance, keywords FROM versions WHERE doc_id=? ORDER BY version_no")) {
                    p.setString(1, id);
                    try (ResultSet r = p.executeQuery()) {
                        while (r.next()) {
                            DocumentVersion v = new DocumentVersion(r.getInt(1), r.getString(2),
                                    TextUtils.normalizeKeywordSet(Arrays.asList(r.getString(5).split(","))),
                                    r.getString(3), r.getInt(4), "");
                            if (first) { search.registerDocument(doc, v); first = false; }
                            else search.addNewVersion(id, v);
                        }
                    }
                }
            }
        }
    }
}
