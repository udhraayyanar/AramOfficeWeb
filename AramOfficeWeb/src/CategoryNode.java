import java.util.*;

/** One node of the department/category tree (root -> department -> category). */
public class CategoryNode {
    private final String name;
    private final CategoryNode parent;
    private final List<CategoryNode> children = new ArrayList<>();
    private final List<String> documentIds = new ArrayList<>();

    public CategoryNode(String name, CategoryNode parent) {
        this.name = name;
        this.parent = parent;
    }

    public String getName() { return name; }
    public CategoryNode getParent() { return parent; }
    public List<CategoryNode> getChildren() { return children; }
    public List<String> getDocumentIds() { return documentIds; }

    public CategoryNode addChild(String childName) {
        CategoryNode existing = findChild(childName);
        if (existing != null) return existing;
        CategoryNode node = new CategoryNode(childName, this);
        children.add(node);
        return node;
    }

    public CategoryNode findChild(String childName) {
        for (CategoryNode c : children) {
            if (c.name.equalsIgnoreCase(childName)) return c;
        }
        return null;
    }

    public void attachDocument(String documentId) {
        if (!documentIds.contains(documentId)) documentIds.add(documentId);
    }
}
