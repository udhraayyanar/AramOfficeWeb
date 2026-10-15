/** Usage: java -cp "out;lib/*" AddUser <username> <password> "<Full Name>" */
public class AddUser {
    public static void main(String[] a) throws Exception {
        Db.init();
        Db.addUser(a[0], a[1], a[2]);
        System.out.println("Added " + a[0]);
    }
}
