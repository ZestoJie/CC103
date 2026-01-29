# CC103 - How to Run as EXE & Share with Others

## 🚀 Current Setup (Your Computer)

### Option 1: Quick Start (What You Have Now)
```bash
# Double-click one of these files:
START.bat                    # Uses Maven commands
START-PORTABLE.bat           # Uses pre-built JARs
```

---

## 📦 Option 2: Share as EXE (For Other Computers)

### For Recipients (People Who Get Your Files)

**Requirements:**
- Java 21 JDK
- Maven 3.9+ (added to PATH)

**Steps:**
1. Copy the `cc103-portable` folder to their computer
2. They double-click `RUN-ME.bat`
3. Everything runs automatically!

### How to Create the Distribution

**On Your Computer:**

```bash
# Step 1: Build the project (you already did this)
mvn clean package

# Step 2: Create portable package
MAKE-PORTABLE.bat
```

This creates: `dist\cc103-portable\` ready to share

---

## 📋 What Files Do What

| File | Purpose | When to Use |
|------|---------|------------|
| `START.bat` | Runs using Maven commands | Your computer (Maven installed) |
| `START-PORTABLE.bat` | Runs using pre-built JARs | Your computer (JAR files in target/) |
| `MAKE-PORTABLE.bat` | Creates distribution package | Before sharing with others |
| `RUN-ME.bat` | Launcher in distribution | Recipients double-click this |

---

## ✅ Complete Checklist

- [x] Application works with `START.bat` ✓
- [x] JAR files created (`mvn package`)
- [x] Can create distribution package
- [x] Recipients can run with `RUN-ME.bat`

**Next Steps:**
```bash
MAKE-PORTABLE.bat          # Create distribution
```

Then share the `dist\cc103-portable\` folder!

---

## 🔧 True EXE Option (Advanced)

To create a true Windows EXE (no Java install needed):

1. Install **Launch4j** (free tool)
2. Use Maven profile to build fat JAR
3. Launch4j wraps JAR as EXE
4. Bundle JDK if needed

Would you like me to set this up? It's more complex but creates a standalone EXE.

---

## 📍 Current Directory Structure

```
cc103/
├── START.bat                 ← Click to run now
├── START-PORTABLE.bat        ← Alternative JAR-based launcher
├── MAKE-PORTABLE.bat         ← Create distribution
├── pom.xml                   ← Maven build config
├── src/                      ← Source code
└── target/                   ← Built JARs
    ├── cc103-1.0.0-server.jar
    └── cc103-app.jar
```

---

Generated: 2026-01-29
