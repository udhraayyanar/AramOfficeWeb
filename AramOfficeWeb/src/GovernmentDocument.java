import java.util.*;

/**
 * Base class for every document type in the office.
 * Encapsulation: the version list and the "current version" pointer can
 * ONLY be changed through addVersion() / setCurrentVersion() - nothing
 * outside this class can silently overwrite history or jump to a version
 * that doesn't exist.
 */
public class GovernmentDocument {
    private final String documentId;
    private final String department;
    private final List<DocumentVersion> versionHistory = new ArrayList<>();
    private DocumentVersion currentVersion;

    public GovernmentDocument(String documentId, String department) {
        this.documentId = documentId;
        this.department = department;
    }

    public String getDocumentId() { return documentId; }
    public String getDepartment() { return department; }

    public List<DocumentVersion> getVersionHistory() {
        return Collections.unmodifiableList(versionHistory);
    }

    public DocumentVersion getCurrentVersion() { return currentVersion; }

    /** The only way to add a version. Validates first, then appends (append-only, never edits). */
    public void addVersion(DocumentVersion v) {
        if (!validateMetadata(v)) {
            throw new IllegalArgumentException("Version " + v.getVersionNumber()
                    + " failed metadata validation for document " + documentId);
        }
        for (DocumentVersion existing : versionHistory) {
            if (existing.getVersionNumber() == v.getVersionNumber()) {
                throw new IllegalArgumentException("Duplicate version number "
                        + v.getVersionNumber() + " for document " + documentId);
            }
        }
        versionHistory.add(v);
    }

    /** The only way to change which version is "current". Returns the old current version (or null). */
    public DocumentVersion setCurrentVersion(int versionNumber) {
        for (DocumentVersion v : versionHistory) {
            if (v.getVersionNumber() == versionNumber) {
                DocumentVersion old = currentVersion;
                currentVersion = v;
                return old;
            }
        }
        throw new NoSuchElementException("Version " + versionNumber
                + " not found in history of " + documentId);
    }

    /**
     * Default validation rule shared by all documents.
     * Subclasses override this for polymorphism (see CircularDocument, ApplicationForm).
     */
    protected boolean validateMetadata(DocumentVersion v) {
        return v != null
                && v.getTitle() != null && !v.getTitle().trim().isEmpty()
                && v.getImportance() >= 1 && v.getImportance() <= 5
                && v.getKeywords() != null && !v.getKeywords().isEmpty();
    }

    public String getDocType() { return "Document"; }
}
