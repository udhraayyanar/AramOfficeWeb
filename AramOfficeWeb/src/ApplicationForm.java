/** An application form. Extra rule: keyword set must include "form" so it is always discoverable as one. */
public class ApplicationForm extends GovernmentDocument {
    public ApplicationForm(String documentId, String department) {
        super(documentId, department);
    }

    @Override
    protected boolean validateMetadata(DocumentVersion v) {
        return super.validateMetadata(v) && v.getKeywords().contains("form");
    }

    @Override
    public String getDocType() { return "ApplicationForm"; }
}
