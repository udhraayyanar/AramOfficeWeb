ARAM OFFICE DOCUMENT PORTAL (web version)

1. Needs JDK (you already have it) and lib\sqlite-jdbc.jar
   (if lib\ is empty, download sqlite-jdbc from
    https://repo1.maven.org/maven2/org/xerial/sqlite-jdbc/3.46.1.3/sqlite-jdbc-3.46.1.3.jar
    and save it as lib\sqlite-jdbc.jar)
2. Double-click run.bat  (or: open cmd in this folder and run it)
3. Open http://localhost:8080

Demo staff logins:  admin / admin123   and   staff / staff123

Database: aramoffice.db (SQLite, created on first run) with tables
users, documents, versions. First run fills it with the 48 synthetic documents.
Search, Trie, ranking and version logic are the same Java classes as the console app.
To reset the data, stop the server and delete aramoffice.db.

NEW IN THIS VERSION
- "All documents" tab: Sort by ID number ascending / descending.
- "Test cases" tab: runs Tests 1-5 (no match, mixed-case prefix, new version replaces index terms
  incl. old-keyword check, duplicate ID, ID ascending/descending). Uses a separate copy of the
  sample data, so your saved documents are not changed.
- src\TestRunner.java is the same file the console project uses: copy it into your console
  project's src folder to get Test 5 there too (menu option 8).
- Add a staff login:
    javac -cp "lib/*" -d out src\*.java
    java --enable-native-access=ALL-UNNAMED -cp "out;lib/*" AddUser <username> <password> "<Full Name>"
- To keep your old users/documents, copy your old aramoffice.db into this folder before run.bat.
- Prefix search is already included (type in the Search box for suggestions).
