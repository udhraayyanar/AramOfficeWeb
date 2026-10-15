/** A department circular. Extra rule: must actually be filed under category "Circular". */
public class CircularDocument extends GovernmentDocument {
    public CircularDocument(String documentId, String department) {
        super(documentId, department);
    }

    @Override
    protected boolean validateMetadata(DocumentVersion v) {
        return super.validateMetadata(v)
                && v.getCategory() != null
                && v.getCategory().equalsIgnoreCase("Circular");
    }

    @Override
    public String getDocType() { return "Circular"; }
}
