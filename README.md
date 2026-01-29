# CC103 JavaFX + Spring Boot Login Application

A modern full-stack desktop application with JavaFX GUI and Spring Boot REST backend.

## Quick Start

### Prerequisites
- Java 21 LTS
- Maven 3.9.12+

### Run Server
```bash
./run-server.bat
```
Server starts on `http://localhost:8080`

### Run Client
```bash
./run-app.bat
```
JavaFX login window appears

## Test Credentials
All test users have password: `password123`
- testuser
- admin
- john
- jane

## Architecture
- **Frontend:** JavaFX 23 with FXML/CSS
- **Backend:** Spring Boot 3.5.0 REST API
- **Database:** H2 in-memory (auto-initialized)
- **Authentication:** HTTP POST to `/api/auth/login`

## Project Structure
```
cc103/
├── src/main/java/com/example/
│   ├── App.java                    # JavaFX entry point
│   ├── ServerApplication.java      # Spring Boot entry point
│   ├── DataInitializer.java        # Auto-create test users
│   ├── controller/
│   │   ├── LoginController.java    # JavaFX login UI logic
│   │   └── AuthController.java     # REST /api/auth endpoints
│   ├── service/UserService.java    # Authentication business logic
│   ├── repository/UserRepository.java
│   └── entity/User.java
├── src/main/resources/
│   ├── application.properties
│   └── com/example/
│       ├── login.fxml
│       └── login.css
├── pom.xml
├── run-server.bat
├── run-app.bat
└── README.md
```

## Key Features
✅ Auto-initialized test users
✅ Modern gradient login UI
✅ Async HTTP requests with Gson
✅ JPA entity persistence
✅ No external database required (H2 in-memory)
✅ Clean, minimal dependencies
