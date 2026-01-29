MySQL setup for CC103 project

1. Create the database and user (run in MySQL/MariaDB shell):

```sql
CREATE DATABASE cc103_db CHARACTER SET utf8mb4 COLLATE utf8mb4_unicode_ci;
CREATE USER 'appuser'@'localhost' IDENTIFIED BY 'change_me_password';
GRANT ALL PRIVILEGES ON cc103_db.* TO 'appuser'@'localhost';
FLUSH PRIVILEGES;
```

2. Update `src/main/resources/application.properties`:

- `spring.datasource.username=appuser`
- `spring.datasource.password=change_me_password`
- Optionally adjust the JDBC URL if MySQL runs on a different host/port.

3. Build and run the application:

```powershell
mvn clean compile
mvn javafx:run   # or your preferred run task
```

4. Notes:
- If you want Hibernate to keep schema between restarts, change `spring.jpa.hibernate.ddl-auto` from `create-drop` to `update` or `none`.
- Ensure MySQL server accepts TCP connections on `localhost:3306` and your user has appropriate privileges.
