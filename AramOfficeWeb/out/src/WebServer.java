import com.sun.net.httpserver.*;
import java.io.*;
import java.net.*;
import java.nio.charset.StandardCharsets;
import java.nio.file.*;
import java.util.*;

/** Web front door: login + JSON API on top of the existing DocumentSearch classes. */
public class WebServer {
    static final DocumentSearch search = new DocumentSearch();
    static final Map<String, String> sessions = new HashMap<>(); // session token -> staff name
    static final String JSON = "application/json";

    public static void main(String[] args) throws Exception {
        Db.init();
        if (Db.count("documents") == 0) {
            SampleDataLoader.load(search);
            for (GovernmentDocument d : search.getAllDocuments()) Db.saveDocument(d);
        } else Db.loadAll(search);
        System.out.println("Loaded " + search.getDocumentCount() + " documents from the database.");
        HttpServer server = HttpServer.create(new InetSocketAddress(8080), 0);
        server.createContext("/", WebServer::handle);
        server.start();
        System.out.println("Open http://localhost:8080 in your browser");
    }

    static void handle(HttpExchange ex) throws IOException {
        try {
            String path = ex.getRequestURI().getPath();
            if (!path.startsWith("/api/")) { serveFile(ex, path); return; }
            Map<String, String> p = params(ex);

            if (path.equals("/api/login")) {
                String fullName = Db.login(p.getOrDefault("username", "").trim(), p.getOrDefault("password", ""));
                if (fullName == null) { send(ex, 401, JSON, "{\"error\":\"Invalid username or password\"}"); return; }
                String token = UUID.randomUUID().toString();
                sessions.put(token, fullName);
                ex.getResponseHeaders().add("Set-Cookie", "sid=" + token + "; Path=/; HttpOnly");
                send(ex, 200, JSON, "{\"ok\":true}");
                return;
            }
            String staff = sessions.get(cookie(ex));
            if (staff == null) { send(ex, 401, JSON, "{\"error\":\"Please log in\"}"); return; }
            String id = p.getOrDefault("id", "").trim();

            switch (path) {
                case "/api/me": send(ex, 200, JSON, "{\"name\":" + q(staff) + "}"); break;
                case "/api/logout": sessions.remove(cookie(ex)); send(ex, 200, JSON, "{\"ok\":true}"); break;
                case "/api/search": {
                    SearchQuery sq = new SearchQuery(Arrays.asList(p.getOrDefault("q", "").split(",")), null, null);
                    List<String> out = new ArrayList<>();
                    for (SearchResult r : search.search(sq)) out.add(row(search.getDocument(r.getDocumentId()), r.getMatchCount()));
                    send(ex, 200, JSON, arr(out)); break;
                }
                case "/api/list": {
                    List<GovernmentDocument> all = new ArrayList<>(search.getAllDocuments());
                    Comparator<GovernmentDocument> byId = Comparator.comparing(GovernmentDocument::getDocumentId, TestRunner.BY_ID_NUMBER);
                    all.sort(p.getOrDefault("order", "asc").equals("desc") ? byId.reversed() : byId);
                    List<String> out = new ArrayList<>();
                    for (GovernmentDocument d : all) out.add(row(d, 0));
                    send(ex, 200, JSON, arr(out)); break;
                }
                case "/api/suggest": {
                    List<String> out = new ArrayList<>();
                    for (String w : search.prefixSuggestions(p.getOrDefault("prefix", ""), 10)) out.add(q(w));
                    send(ex, 200, JSON, arr(out)); break;
                }
                case "/api/doc": {
                    GovernmentDocument d = search.getDocument(id);
                    if (d == null) { send(ex, 404, JSON, "{\"error\":\"Document not found\"}"); break; }
                    List<String> vs = new ArrayList<>();
                    for (DocumentVersion v : d.getVersionHistory())
                        vs.add("{\"no\":" + v.getVersionNumber() + ",\"title\":" + q(v.getTitle()) + ",\"category\":" + q(v.getCategory())
                                + ",\"importance\":" + v.getImportance() + ",\"keywords\":" + q(String.join(", ", v.getKeywords())) + "}");
                    send(ex, 200, JSON, "{\"id\":" + q(id) + ",\"type\":" + q(d.getDocType()) + ",\"department\":" + q(d.getDepartment())
                            + ",\"current\":" + d.getCurrentVersion().getVersionNumber() + ",\"versions\":" + arr(vs) + "}");
                    break;
                }
                case "/api/add": {
                    String dept = p.getOrDefault("department", "").trim(), cat = p.getOrDefault("category", "").trim();
                    if (id.isEmpty() || dept.isEmpty()) throw new IllegalArgumentException("Document ID and department are required");
                    GovernmentDocument doc = cat.equalsIgnoreCase("Circular") ? new CircularDocument(id, dept)
                            : cat.equalsIgnoreCase("ApplicationForm") ? new ApplicationForm(id, dept) : new GovernmentDocument(id, dept);
                    search.registerDocument(doc, version(1, p, cat));
                    Db.saveDocument(doc);
                    send(ex, 200, JSON, "{\"ok\":true}"); break;
                }
                case "/api/version": {
                    GovernmentDocument d = search.getDocument(id);
                    if (d == null) throw new IllegalArgumentException("Document not found");
                    DocumentVersion v = version(d.getVersionHistory().size() + 1, p, p.getOrDefault("category", "").trim());
                    search.addNewVersion(id, v);
                    Db.saveVersion(id, v);
                    send(ex, 200, JSON, "{\"ok\":true}"); break;
                }
                case "/api/tests": {
                    DocumentSearch fresh = new DocumentSearch();   // separate copy: never touches the real database
                    SampleDataLoader.load(fresh);
                    ByteArrayOutputStream buf = new ByteArrayOutputStream();
                    PrintStream console = System.out;
                    System.setOut(new PrintStream(buf, true, StandardCharsets.UTF_8));
                    try { TestRunner.run(fresh); } finally { System.setOut(console); }
                    send(ex, 200, JSON, "{\"output\":" + q(buf.toString(StandardCharsets.UTF_8)) + "}"); break;
                }
                case "/api/tree": {
                    List<String> depts = new ArrayList<>();
                    for (CategoryNode dn : search.getCategoryTree().getRoot().getChildren()) {
                        List<String> cats = new ArrayList<>();
                        for (CategoryNode cn : dn.getChildren())
                            cats.add("{\"name\":" + q(cn.getName()) + ",\"count\":" + cn.getDocumentIds().size() + "}");
                        depts.add("{\"name\":" + q(dn.getName()) + ",\"categories\":" + arr(cats) + "}");
                    }
                    send(ex, 200, JSON, arr(depts)); break;
                }
                default: send(ex, 404, JSON, "{\"error\":\"Unknown API\"}");
            }
        } catch (RuntimeException e) {
            send(ex, 400, JSON, "{\"error\":" + q(String.valueOf(e.getMessage())) + "}");
        } catch (Exception e) {
            e.printStackTrace();
            send(ex, 500, JSON, "{\"error\":\"Server error\"}");
        }
    }

    static DocumentVersion version(int no, Map<String, String> p, String cat) {
        int imp;
        try { imp = Integer.parseInt(p.getOrDefault("importance", "3").trim()); }
        catch (NumberFormatException e) { throw new IllegalArgumentException("Importance must be a number from 1 to 5"); }
        Set<String> kws = TextUtils.normalizeKeywordSet(Arrays.asList(p.getOrDefault("keywords", "").split(",")));
        String[] pool = SampleDataLoader.poolFor(cat);
        if (pool == null) throw new IllegalArgumentException("Unknown category: " + cat);
        boolean related = false;
        for (String w : pool) if (kws.contains(w)) { related = true; break; }
        if (!related) throw new IllegalArgumentException("Keywords must be related to the category " + cat
                + ". Include at least one of: " + String.join(", ", pool));
        if (cat.equalsIgnoreCase("ApplicationForm") && !kws.contains("form"))
            throw new IllegalArgumentException("Application forms must include the keyword \"form\"");
        return new DocumentVersion(no, p.getOrDefault("title", "").trim(), kws, cat, imp, "");
    }

    static String row(GovernmentDocument d, int matches) {
        DocumentVersion v = d.getCurrentVersion();
        return "{\"id\":" + q(d.getDocumentId()) + ",\"department\":" + q(d.getDepartment()) + ",\"category\":" + q(v.getCategory())
                + ",\"title\":" + q(v.getTitle()) + ",\"importance\":" + v.getImportance() + ",\"version\":" + v.getVersionNumber()
                + ",\"matches\":" + matches + "}";
    }

    static String arr(List<String> items) { return "[" + String.join(",", items) + "]"; }

    static String q(String s) {
        return "\"" + s.replace("\\", "\\\\").replace("\"", "\\\"").replace("\n", "\\n").replace("\r", "") + "\"";
    }

    static Map<String, String> params(HttpExchange ex) throws IOException {
        String raw = ex.getRequestURI().getRawQuery();
        if (ex.getRequestMethod().equals("POST")) raw = new String(ex.getRequestBody().readAllBytes(), StandardCharsets.UTF_8);
        Map<String, String> m = new HashMap<>();
        if (raw == null || raw.isEmpty()) return m;
        for (String kv : raw.split("&")) {
            String[] a = kv.split("=", 2);
            m.put(URLDecoder.decode(a[0], StandardCharsets.UTF_8), a.length > 1 ? URLDecoder.decode(a[1], StandardCharsets.UTF_8) : "");
        }
        return m;
    }

    static String cookie(HttpExchange ex) {
        String c = ex.getRequestHeaders().getFirst("Cookie");
        if (c == null) return "";
        for (String part : c.split(";")) { String s = part.trim(); if (s.startsWith("sid=")) return s.substring(4); }
        return "";
    }

    static void serveFile(HttpExchange ex, String path) throws IOException {
        boolean loggedIn = sessions.containsKey(cookie(ex));
        if (path.equals("/")) path = loggedIn ? "/app.html" : "/login.html";
        if (path.equals("/app.html") && !loggedIn) path = "/login.html";
        Path f = Paths.get("web", path).normalize();
        if (!f.startsWith("web") || !Files.isRegularFile(f)) { send(ex, 404, "text/plain", "Not found"); return; }
        String type = path.endsWith(".html") ? "text/html; charset=utf-8" : path.endsWith(".css") ? "text/css" : "application/octet-stream";
        send(ex, 200, type, Files.readAllBytes(f));
    }

    static void send(HttpExchange ex, int code, String type, String body) throws IOException {
        send(ex, code, type, body.getBytes(StandardCharsets.UTF_8));
    }

    static void send(HttpExchange ex, int code, String type, byte[] body) throws IOException {
        ex.getResponseHeaders().set("Content-Type", type);
        ex.sendResponseHeaders(code, body.length);
        try (OutputStream os = ex.getResponseBody()) { os.write(body); }
    }
}
