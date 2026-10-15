import java.io.IOException;
import java.util.*;

public class Main {
    private static final Scanner sc = new Scanner(System.in);
    private static final DocumentSearch search = new DocumentSearch();
    private static final String DATA_FILE = "aram_office_data.txt";

    public static void main(String[] args) {
        boolean loadedFromDisk = false;
        try {
            loadedFromDisk = PersistenceManager.load(search, DATA_FILE);
        } catch (IOException e) {
            System.out.println("Could not read saved data (" + e.getMessage() + "); starting fresh.");
        }

        if (loadedFromDisk) {
            System.out.println("Loaded " + search.getDocumentCount()
                    + " documents from saved file (" + DATA_FILE + ").\n");
        } else {
            System.out.println("No saved data found. Loading synthetic dataset...");
            SampleDataLoader.load(search);
            System.out.println("Loaded " + search.getDocumentCount() + " documents.\n");
        }

        boolean running = true;
        while (running) {
            printMenu();
            String choice = sc.nextLine().trim();
            switch (choice) {
                case "1": addDocument(); break;
                case "2": addVersion(); break;
                case "3": keywordSearch(); break;
                case "4": prefixSearch(); break;
                case "5": viewDocument(); break;
                case "6": listByCategory(); break;
                case "7": printCategoryTree(); break;
                case "8": TestRunner.run(search); System.out.println(); break;
                case "9": listAllDocuments(); break;
                case "10": topKSearch(); break;
                case "11": running = false; break;
                default: System.out.println("Invalid choice.\n");
            }
        }

        try {
            PersistenceManager.save(search, DATA_FILE);
            System.out.println("Saved " + search.getDocumentCount() + " documents to " + DATA_FILE + ".");
        } catch (IOException e) {
            System.out.println("Could not save data: " + e.getMessage());
        }
        System.out.println("Goodbye.");
    }

    private static void printMenu() {
        System.out.println("========= Aram Office Document System =========");
        System.out.println("1. Add new document");
        System.out.println("2. Add new version to existing document");
        System.out.println("3. Keyword search");
        System.out.println("4. Prefix suggestions");
        System.out.println("5. View document details / version history");
        System.out.println("6. List documents by department + category");
        System.out.println("7. Print category tree");
        System.out.println("8. Run documented test cases");
        System.out.println("9. List all documents");
        System.out.println("10. Top-K keyword search");
        System.out.println("11. Exit");
        System.out.print("Choose an option: ");
    }

    private static void addDocument() {
        try {
            System.out.print("Document ID: "); String id = sc.nextLine().trim();
            System.out.print("Department: "); String dept = sc.nextLine().trim();
            System.out.print("Category (Circular/ApplicationForm/ServiceInstruction/Notification/Guideline/Report): ");
            String cat = sc.nextLine().trim();
            System.out.print("Title: "); String title = sc.nextLine().trim();
            System.out.print("Keywords (comma-separated): ");
            List<String> kws = Arrays.asList(sc.nextLine().split(","));
            System.out.print("Importance (1-5): "); int importance = Integer.parseInt(sc.nextLine().trim());

            GovernmentDocument doc;
            if (cat.equalsIgnoreCase("Circular")) doc = new CircularDocument(id, dept);
            else if (cat.equalsIgnoreCase("ApplicationForm")) doc = new ApplicationForm(id, dept);
            else doc = new GovernmentDocument(id, dept);

            DocumentVersion v1 = new DocumentVersion(1, title,
                    TextUtils.normalizeKeywordSet(kws), cat, importance, "");
            search.registerDocument(doc, v1);
            System.out.println("Document " + id + " added.\n");
        } catch (DuplicateDocumentException e) {
            System.out.println("Error: " + e.getMessage() + "\n");
        } catch (IllegalArgumentException e) {
            System.out.println("Rejected: " + e.getMessage() + "\n");
        } catch (Exception e) {
            System.out.println("Input error: " + e.getMessage() + "\n");
        }
    }

    private static void addVersion() {
        try {
            System.out.print("Document ID to update: "); String id = sc.nextLine().trim();
            GovernmentDocument doc = search.getDocument(id);
            if (doc == null) { System.out.println("No such document.\n"); return; }

            int nextVersion = doc.getVersionHistory().size() + 1;
            System.out.print("New title: "); String title = sc.nextLine().trim();
            System.out.print("New keywords (comma-separated): ");
            List<String> kws = Arrays.asList(sc.nextLine().split(","));
            System.out.print("Category: "); String cat = sc.nextLine().trim();
            System.out.print("Importance (1-5): "); int importance = Integer.parseInt(sc.nextLine().trim());

            DocumentVersion v = new DocumentVersion(nextVersion, title,
                    TextUtils.normalizeKeywordSet(kws), cat, importance, "");
            search.addNewVersion(id, v);
            System.out.println("Version " + nextVersion + " is now current for " + id + ".\n");
        } catch (IllegalArgumentException e) {
            System.out.println("Rejected: " + e.getMessage() + "\n");
        } catch (Exception e) {
            System.out.println("Input error: " + e.getMessage() + "\n");
        }
    }

    private static void keywordSearch() {
        System.out.print("Keywords (comma-separated): ");
        List<String> kws = Arrays.asList(sc.nextLine().split(","));
        SearchQuery q = new SearchQuery(kws, null, null);
        List<SearchResult> results = search.search(q);
        if (results.isEmpty()) { System.out.println("No matching documents.\n"); return; }
        for (SearchResult r : results) System.out.println(r);
        System.out.println();
    }

    private static void prefixSearch() {
        System.out.print("Prefix: ");
        String prefix = sc.nextLine().trim();
        List<String> suggestions = search.prefixSuggestions(prefix, 15);
        System.out.println(suggestions.isEmpty() ? "No suggestions.\n" : suggestions + "\n");
    }

    private static void viewDocument() {
        System.out.print("Document ID: ");
        String id = sc.nextLine().trim();
        GovernmentDocument doc = search.getDocument(id);
        if (doc == null) { System.out.println("No such document.\n"); return; }
        System.out.println("ID: " + doc.getDocumentId() + " | Type: " + doc.getDocType()
                + " | Department: " + doc.getDepartment());
        System.out.println("Current version: " + doc.getCurrentVersion());
        System.out.println("Full history:");
        for (DocumentVersion v : doc.getVersionHistory()) System.out.println("  " + v);
        System.out.println();
    }

    private static void listByCategory() {
        System.out.print("Department: "); String dept = sc.nextLine().trim();
        System.out.print("Category: "); String cat = sc.nextLine().trim();
        List<GovernmentDocument> docs = search.byDepartmentAndCategory(dept, cat);
        if (docs.isEmpty()) { System.out.println("No documents found.\n"); return; }
        for (GovernmentDocument d : docs) {
            System.out.println(d.getDocumentId() + " | " + d.getCurrentVersion().getTitle());
        }
        System.out.println();
    }

    private static void printCategoryTree() {
        search.getCategoryTree().print();
        System.out.println();
    }

    private static void listAllDocuments() {
        List<GovernmentDocument> docs = new ArrayList<>(search.getAllDocuments());
        docs.sort(Comparator.comparing(GovernmentDocument::getDocumentId));
        System.out.println("Total documents: " + docs.size());
        for (GovernmentDocument d : docs) {
            System.out.println(d.getDocumentId() + " | " + d.getDepartment() + " | "
                    + d.getCurrentVersion().getCategory() + " | " + d.getCurrentVersion().getTitle());
        }
        System.out.println();
    }

    private static void topKSearch() {
        try {
            System.out.print("Keywords (comma-separated): ");
            List<String> kws = Arrays.asList(sc.nextLine().split(","));
            System.out.print("How many top results (k): ");
            int k = Integer.parseInt(sc.nextLine().trim());

            SearchQuery q = new SearchQuery(kws, null, null);
            List<SearchResult> results = search.searchTopK(q, k);
            if (results.isEmpty()) { System.out.println("No matching documents.\n"); return; }
            for (SearchResult r : results) System.out.println(r);
            System.out.println();
        } catch (Exception e) {
            System.out.println("Input error: " + e.getMessage() + "\n");
        }
    }
}
