ARAM OFFICE DOCUMENT PORTAL (web version) - XAMPP MySQL edition

1. Needs: JDK, XAMPP, and the JDBC driver lib\mariadb-java-client-3.4.1.jar
   run.bat downloads the driver automatically the first time (internet needed once).
   If that fails, download it manually and save it in the lib folder:
   https://repo1.maven.org/maven2/org/mariadb/jdbc/mariadb-java-client/3.4.1/mariadb-java-client-3.4.1.jar

2. Start XAMPP Control Panel -> click Start next to MySQL (Apache is only needed for phpMyAdmin).

3. Create the database (recommended):
   open http://localhost/phpmyadmin -> Import -> Choose File -> aramoffice.sql -> Go
   This creates the database "aramoffice" with tables users, documents, versions,
   the two demo logins and the 48 synthetic documents.
   (If you skip this step, the program creates the database and tables itself on first run.)

4. Double-click run.bat, then open http://localhost:8080

Demo staff logins:  admin / admin123   and   staff / staff123

Connection settings (src\Db.java) - XAMPP defaults:
   host localhost, port 3306, database aramoffice, user root, empty password.
   If your MySQL root has a password or another port, either edit the constants at the top
   of src\Db.java, or set environment variables ARAM_DB_HOST, ARAM_DB_PORT, ARAM_DB_NAME,
   ARAM_DB_USER, ARAM_DB_PASS before running.

Reset the data: in phpMyAdmin drop the database "aramoffice" and import aramoffice.sql again.

Add a staff login:
    javac -cp "lib/*" -d out src\*.java
    java -cp "out;lib/*" AddUser <username> <password> "<Full Name>"

Other features (unchanged): All documents tab with ID sorting, Test cases tab (uses a separate
in-memory copy of the sample data, so your database is not changed), prefix search suggestions.
