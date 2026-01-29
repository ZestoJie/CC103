# PostgreSQL Setup for CC103 Project

## ✅ What Was Done

Your project has been updated to use **PostgreSQL** instead of H2 in-memory database.

### Changes Made:
- ✅ Updated `pom.xml` - Replaced H2 with PostgreSQL 42.7.3 driver
- ✅ Updated `application.properties` - Changed database connection to PostgreSQL
- ✅ Compiled successfully - All dependencies resolved ✓

---

## 🚀 Quick Setup (3 Steps)

### Step 1: Install PostgreSQL
Download from: **https://www.postgresql.org/download/windows/**

During installation:
- Choose default options
- **REMEMBER THE PASSWORD** you set for "postgres" user
- Default port is 5432 (keep it)

### Step 2: Create Database
Open **pgAdmin** (comes with PostgreSQL):
1. Right-click "Databases" → Create → Database
2. Name: `cc103_db`
3. Owner: `postgres`
4. Click Create

Or use Command Prompt:
```cmd
psql -U postgres
CREATE DATABASE cc103_db;
\q
```

### Step 3: Update Password
Edit: `src/main/resources/application.properties`

Find this line:
```
spring.datasource.password=YOUR_POSTGRES_PASSWORD
```

Replace `YOUR_POSTGRES_PASSWORD` with the password you set during PostgreSQL installation.

---

## ✅ Verify Setup

### Test 1: Compile
```cmd
&'C:\Users\rhejie carl\.maven\maven-3.9.12(1)\bin\mvn.cmd' clean compile
```
Expected: `BUILD SUCCESS`

### Test 2: Start Server
```cmd
&'C:\Users\rhejie carl\.maven\maven-3.9.12(1)\bin\mvn.cmd' spring-boot:run
```

Wait for:
```
Tomcat initialized with port 8080
Started ServerApplication
```

### Test 3: Start App
In another terminal:
```cmd
&'C:\Users\rhejie carl\.maven\maven-3.9.12(1)\bin\mvn.cmd' javafx:run
```

### Test 4: Login
Use credentials:
- Username: `testuser`
- Password: `password123`

If successful → Database is working! ✅

---

## 📊 Check Database

Open **pgAdmin**:
1. Expand "Databases" → "cc103_db"
2. Expand "Schemas" → "public" → "Tables"
3. You should see **"users"** table with columns:
   - id
   - username
   - password
   - email

---

## ❌ Troubleshooting

### Error: "Connection refused"
- PostgreSQL service not running
- Windows: Open Services (services.msc) → Find "postgresql-x64-15"
- Right-click → Start

### Error: "password authentication failed"
- Password in application.properties is wrong
- Double-check the password you set during install

### Error: "database 'cc103_db' does not exist"
- Create the database using pgAdmin (see Step 2 above)

### Error: "Driver not found"
```cmd
&'C:\Users\rhejie carl\.maven\maven-3.9.12(1)\bin\mvn.cmd' clean install
```

---

## 📝 Key Differences from H2

### Before (H2 In-Memory):
- Data in RAM only
- Lost when server stops
- No external setup needed
- Fast for testing

### Now (PostgreSQL):
- ✅ Data persists on disk
- ✅ Data survives server restart
- ✅ Professional production database
- ✅ Can be shared across multiple apps
- ✅ Scalable to large datasets

---

## 🔧 Connection String

Your connection string:
```
jdbc:postgresql://localhost:5432/cc103_db
```

Breaking it down:
- `localhost` = Your computer (PostgreSQL server location)
- `5432` = Default PostgreSQL port
- `cc103_db` = Your database name

---

## 📂 PostgreSQL Data Location

Windows default:
```
C:\Program Files\PostgreSQL\15\data
```

---

## ✨ Next Steps

1. Install PostgreSQL from link above
2. Create `cc103_db` database in pgAdmin
3. Update password in `application.properties`
4. Run tests above
5. Done! Your app now uses real persistent database ✅

---

## 💡 Want to Switch Back to H2?

If you want to go back to H2 in-memory (for testing):

1. Edit `pom.xml` - Replace PostgreSQL with H2:
```xml
<dependency>
    <groupId>com.h2database</groupId>
    <artifactId>h2</artifactId>
    <version>2.3.232</version>
    <scope>runtime</scope>
</dependency>
```

2. Edit `application.properties`:
```
spring.datasource.url=jdbc:h2:mem:testdb
spring.datasource.username=sa
spring.datasource.password=
spring.jpa.database-platform=org.hibernate.dialect.H2Dialect
```

3. Run:
```cmd
&'C:\Users\rhejie carl\.maven\maven-3.9.12(1)\bin\mvn.cmd' clean compile
```

---

**Questions?** Check `DATABASE-SETUP.txt` for detailed explanations.
