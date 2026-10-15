public class DuplicateDocumentException extends RuntimeException {
    public DuplicateDocumentException(String documentId) {
        super("Document ID already exists: " + documentId);
    }
}
