import java.security.MessageDigest;
import java.sql.*;
import java.util.*;

/** MySQL (XAMPP) database layer: staff accounts + documents + versions. */
public class Db {
    // ---- XAMPP defaults: change here (or set environment variables) if yours differ ----
    static final String HOST = env("ARAM_DB_HOST", "localhost");
    static final String PORT = env("ARAM_DB_PORT", "3306");
    static final String NAME = env("ARAM_DB_NAME", "aramoffice");
    static final String USER = env("ARAM_DB_USER", "root");
    static final String PASS = env("ARAM_DB_PASS", "");          // XAMPP root has no password by default
    static final String OPTS = "?useUnicode=true&characterEncoding=utf8";

    static String env(String key, String def) {
        String v = System.getenv(key);
        return (v == null || v.isEmpty()) ? def : v;
    }

    static Connection conn() throws SQLException {
        return DriverManager.getConnection("jdbc:mariadb://" + HOST + ":" + PORT + "/" + NAME + OPTS, USER, PASS);
    }

    /** Creates the database if the SQL file has not been imported yet. */
    static void ensureDatabase() throws SQLException {
        try (Connection c = DriverManager.getConnection("jdbc:mariadb://" + HOST + ":" + PORT + "/" + OPTS, USER, PASS);
             Statement s = c.createStatement()) {
            s.execute("CREATE DATABASE IF NOT EXISTS `" + NAME + "` CHARACTER SET utf8mb4 COLLATE utf8mb4_unicode_ci");
        } catch (SQLNonTransientConnectionException e) {
            throw new SQLException("Cannot reach MySQL at " + HOST + ":" + PORT
                    + ". Open the XAMPP Control Panel and click Start next to MySQL, then run again.", e);
        }
    }

    static void init() throws Exception {
        ensureDatabase();
        try (Connection c = conn(); Statement s = c.createStatement()) {
            s.execute("CREATE TABLE IF NOT EXISTS users("
                    + "username VARCHAR(50) COLLATE utf8mb4_bin NOT NULL PRIMARY KEY, "
                    + "salt VARCHAR(64) NOT NULL, hash VARCHAR(64) NOT NULL, full_name VARCHAR(100) NOT NULL"
                    + ") ENGINE=InnoDB DEFAULT CHARSET=utf8mb4");
            s.execute("CREATE TABLE IF NOT EXISTS documents("
                    + "id VARCHAR(50) COLLATE utf8mb4_bin NOT NULL PRIMARY KEY, "
                    + "department VARCHAR(100) NOT NULL, doc_type VARCHAR(50) NOT NULL"
                    + ") ENGINE=InnoDB DEFAULT CHARSET=utf8mb4");
            s.execute("CREATE TABLE IF NOT EXISTS versions("
                    + "doc_id VARCHAR(50) COLLATE utf8mb4_bin NOT NULL, version_no INT NOT NULL, "
                    + "title VARCHAR(500) NOT NULL, category VARCHAR(50) NOT NULL, importance INT NOT NULL, keywords TEXT NOT NULL, "
                    + "PRIMARY KEY(doc_id, version_no), "
                    + "CONSTRAINT fk_versions_doc FOREIGN KEY (doc_id) REFERENCES documents(id) ON DELETE CASCADE"
                    + ") ENGINE=InnoDB DEFAULT CHARSET=utf8mb4");
        }
        if (count("users") == 0) {
            addUser("admin", "admin123", "Office Admin");
            addUser("staff", "staff123", "Office Staff");
        }
    }

    static int count(String table) throws SQLException {
        try (Connection c = conn(); Statement s = c.createStatement(); ResultSet r = s.executeQuery("SELECT COUNT(*) FROM " + table)) {
            r.next();
            return r.getInt(1);
        }
    }

    static String hash(String salt, String pw) throws Exception {
        return HexFormat.of().formatHex(MessageDigest.getInstance("SHA-256").digest((salt + pw).getBytes("UTF-8")));
    }

    static void addUser(String user, String pw, String fullName) throws Exception {
        String salt = UUID.randomUUID().toString();
        try (Connection c = conn(); PreparedStatement p = c.prepareStatement("INSERT INTO users(username, salt, hash, full_name) VALUES(?,?,?,?)")) {
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
        try (Connection c = conn(); PreparedStatement p = c.prepareStatement("INSERT IGNORE INTO documents(id, department, doc_type) VALUES(?,?,?)")) {
            p.setString(1, d.getDocumentId()); p.setString(2, d.getDepartment()); p.setString(3, d.getDocType());
            p.executeUpdate();
        }
        for (DocumentVersion v : d.getVersionHistory()) saveVersion(d.getDocumentId(), v);
    }

    static void saveVersion(String id, DocumentVersion v) throws SQLException {
        try (Connection c = conn(); PreparedStatement p = c.prepareStatement(
                "INSERT IGNORE INTO versions(doc_id, version_no, title, category, importance, keywords) VALUES(?,?,?,?,?,?)")) {
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
