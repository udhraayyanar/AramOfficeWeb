import java.sql.*;
import java.util.*;

public class ShowDb {
    public static void main(String[] a) throws Exception {
        try (Connection c = Db.conn(); Statement s = c.createStatement()) {
            if (a.length > 0) { show(s, a[0]); return; }   // your own SELECT query
            show(s, "SELECT username, full_name, substr(hash,1,12) || '...' AS password_hash FROM users");
            show(s, "SELECT * FROM documents ORDER BY id");
            show(s, "SELECT doc_id, version_no, title, category, importance, keywords FROM versions ORDER BY doc_id, version_no");
        }
    }

    static void show(Statement s, String sql) throws SQLException {
        System.out.println("\n> " + sql);
        List<String[]> rows = new ArrayList<>();
        try (ResultSet r = s.executeQuery(sql)) {
            int n = r.getMetaData().getColumnCount();
            String[] head = new String[n];
            for (int i = 0; i < n; i++) head[i] = r.getMetaData().getColumnName(i + 1);
            rows.add(head);
            while (r.next()) {
                String[] row = new String[n];
                for (int i = 0; i < n; i++) row[i] = String.valueOf(r.getString(i + 1));
                rows.add(row);
            }
        }
        int n = rows.get(0).length;
        int[] w = new int[n];
        for (String[] row : rows) for (int i = 0; i < n; i++) w[i] = Math.max(w[i], Math.min(row[i].length(), 45));
        for (int k = 0; k < rows.size(); k++) {
            StringBuilder sb = new StringBuilder();
            for (int i = 0; i < n; i++) {
                String v = rows.get(k)[i];
                if (v.length() > 45) v = v.substring(0, 42) + "...";
                sb.append(String.format("%-" + w[i] + "s  ", v));
            }
            System.out.println(sb);
            if (k == 0) System.out.println("-".repeat(sb.length()));
        }
        System.out.println("(" + (rows.size() - 1) + " rows)");
    }
}