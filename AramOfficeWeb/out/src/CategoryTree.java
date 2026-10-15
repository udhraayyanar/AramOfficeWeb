/** Department / category hierarchy for Aram Office. */
public class CategoryTree {
    private final CategoryNode root = new CategoryNode("Aram Office", null);

    public CategoryNode getRoot() { return root; }

    public CategoryNode getOrCreatePath(String department, String category) {
        CategoryNode deptNode = root.addChild(department);
        return deptNode.addChild(category);
    }

    public CategoryNode findPath(String department, String category) {
        CategoryNode deptNode = root.findChild(department);
        if (deptNode == null) return null;
        return deptNode.findChild(category);
    }

    public void print() {
        printNode(root, 0);
    }

    private void printNode(CategoryNode node, int depth) {
        String tag = node.getDocumentIds().isEmpty() ? "" : " (" + node.getDocumentIds().size() + " docs)";
        System.out.println("  ".repeat(depth) + node.getName() + tag);
        for (CategoryNode c : node.getChildren()) printNode(c, depth + 1);
    }
}
