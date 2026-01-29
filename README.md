================================================================================
FULL STACK JAVAFX + SPRING BOOT APP
(MYSQL DATABASE VERSION)
Created: 2026-01-29
Version: 1.1 (MySQL Fixed & Verified)
================================================================================
OVERVIEW
This guide shows how to build a complete full-stack application with:
JavaFX Desktop GUI (Frontend)
Spring Boot REST API (Backend)
MySQL Community Server (Persistent Database)
HTTP Client Communication (JSON-based)
================================================================================
PREREQUISITES (INSTALL THESE FIRST)
Java 21 JDK
URL: https://www.oracle.com/java/technologies/downloads/
Verify:
java -version
Maven 3.9.12+
URL: https://maven.apache.org/download.cgi
Verify:
mvn -version
MySQL Community Server 8+
URL: https://dev.mysql.com/downloads/mysql/
Port: 3306
Username: root
VS Code (Recommended)
URL: https://code.visualstudio.com/
================================================================================
STEP 1: CREATE DATABASE
Open MySQL Command Line or MySQL Workbench and run:
CREATE DATABASE cc103_db;
================================================================================
STEP 2: CREATE PROJECT STRUCTURE
Command Prompt:
mkdir cc103-app
cd cc103-app
Create folders:
src\main\java\com\example
src\main\java\com\example\controller
src\main\java\com\example\entity
src\main\java\com\example\repository
src\main\java\com\example\service
src\main\resources\com\example\
================================================================================
STEP 3: CREATE pom.xml (MAVEN CONFIGURATION)
File: pom.xml
<modelVersion>4.0.0</modelVersion>

<groupId>com.example</groupId>
<artifactId>cc103</artifactId>
<version>1.0.0</version>

<properties>
    <maven.compiler.source>21</maven.compiler.source>
    <maven.compiler.target>21</maven.compiler.target>
    <project.build.sourceEncoding>UTF-8</project.build.sourceEncoding>
</properties>

<dependencies>

    <!-- JavaFX -->
    <dependency>
        <groupId>org.openjfx</groupId>
        <artifactId>javafx-controls</artifactId>
        <version>23</version>
    </dependency>
    <dependency>
        <groupId>org.openjfx</groupId>
        <artifactId>javafx-fxml</artifactId>
        <version>23</version>
    </dependency>

    <!-- Spring Boot -->
    <dependency>
        <groupId>org.springframework.boot</groupId>
        <artifactId>spring-boot-starter-web</artifactId>
        <version>3.5.0</version>
    </dependency>
    <dependency>
        <groupId>org.springframework.boot</groupId>
        <artifactId>spring-boot-starter-data-jpa</artifactId>
        <version>3.5.0</version>
    </dependency>

    <!-- MySQL Connector -->
    <dependency>
        <groupId>com.mysql</groupId>
        <artifactId>mysql-connector-j</artifactId>
        <version>9.1.0</version>
    </dependency>

    <!-- JSON -->
    <dependency>
        <groupId>com.google.code.gson</groupId>
        <artifactId>gson</artifactId>
        <version>2.10.1</version>
    </dependency>

    <!-- Jakarta Persistence -->
    <dependency>
        <groupId>jakarta.persistence</groupId>
        <artifactId>jakarta.persistence-api</artifactId>
        <version>3.1.0</version>
    </dependency>

</dependencies>

<build>
    <plugins>
        <plugin>
            <groupId>org.apache.maven.plugins</groupId>
            <artifactId>maven-compiler-plugin</artifactId>
            <version>3.13.0</version>
        </plugin>
        <plugin>
            <groupId>org.openjfx</groupId>
            <artifactId>javafx-maven-plugin</artifactId>
            <version>0.0.8</version>
            <configuration>
                <mainClass>com.example.App</mainClass>
            </configuration>
        </plugin>
        <plugin>
            <groupId>org.springframework.boot</groupId>
            <artifactId>spring-boot-maven-plugin</artifactId>
            <version>3.5.0</version>
        </plugin>
    </plugins>
</build>

================================================================================
STEP 4: DATABASE ENTITY
File: src/main/java/com/example/entity/User.java
package com.example.entity;
import jakarta.persistence.*;
@Entity
@Table(name = "users")
public class User {
@Id
@GeneratedValue(strategy = GenerationType.IDENTITY)
private Long id;

@Column(unique = true, nullable = false)
private String username;

private String password;
private String email;

public User() {}

public User(String username, String password, String email) {
    this.username = username;
    this.password = password;
    this.email = email;
}

public Long getId() { return id; }
public String getUsername() { return username; }
public String getPassword() { return password; }
public String getEmail() { return email; }

}
================================================================================
STEP 5: REPOSITORY
File: src/main/java/com/example/repository/UserRepository.java
package com.example.repository;
import org.springframework.data.jpa.repository.JpaRepository;
import com.example.entity.User;
public interface UserRepository extends JpaRepository<User, Long> {
User findByUsername(String username);
boolean existsByUsername(String username);
}
================================================================================
STEP 6: SERVICE
File: src/main/java/com/example/service/UserService.java
package com.example.service;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import com.example.entity.User;
import com.example.repository.UserRepository;
@Service
public class UserService {
@Autowired
private UserRepository userRepository;

public User authenticate(String username, String password) {
    User user = userRepository.findByUsername(username);
    if (user != null && user.getPassword().equals(password)) {
        return user;
    }
    return null;
}

}
================================================================================
STEP 7: REST CONTROLLER
File: src/main/java/com/example/controller/AuthController.java
package com.example.controller;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.web.bind.annotation.;
import com.example.service.UserService;
import com.example.entity.User;
import com.google.gson.;
@RestController
@RequestMapping("/api/auth")
public class AuthController {
@Autowired
private UserService userService;

@PostMapping("/login")
public String login(@RequestBody String body) {
    JsonObject json = JsonParser.parseString(body).getAsJsonObject();
    String username = json.get("username").getAsString();
    String password = json.get("password").getAsString();

    User user = userService.authenticate(username, password);
    JsonObject response = new JsonObject();

    if (user != null) {
        response.addProperty("success", true);
        response.addProperty("userId", user.getId());
        response.addProperty("email", user.getEmail());
    } else {
        response.addProperty("success", false);
        response.addProperty("message", "Invalid credentials");
    }
    return response.toString();
}

}
================================================================================
STEP 8: DATA INITIALIZER
File: src/main/java/com/example/DataInitializer.java
package com.example;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.CommandLineRunner;
import org.springframework.stereotype.Component;
import com.example.entity.User;
import com.example.repository.UserRepository;
@Component
public class DataInitializer implements CommandLineRunner {
@Autowired
private UserRepository userRepository;

@Override
public void run(String... args) {
    if (userRepository.count() == 0) {
        userRepository.save(new User("testuser", "password123", "test@example.com"));
        userRepository.save(new User("admin", "password123", "admin@example.com"));
        System.out.println("✅ MySQL test users inserted");
    }
}

}
================================================================================
STEP 9: SPRING BOOT ENTRY POINT
File: src/main/java/com/example/ServerApplication.java
package com.example;
import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
@SpringBootApplication
public class ServerApplication {
public static void main(String[] args) {
SpringApplication.run(ServerApplication.class, args);
}
}
================================================================================
STEP 10: APPLICATION PROPERTIES (MYSQL)
File: src/main/resources/application.properties
server.port=8080
spring.datasource.url=jdbc:mysql://localhost:3306/cc103_db?useSSL=false&serverTimezone=UTC
spring.datasource.username=root
spring.datasource.password=YOUR_MYSQL_PASSWORD
spring.datasource.driver-class-name=com.mysql.cj.jdbc.Driver
spring.jpa.hibernate.ddl-auto=update
spring.jpa.show-sql=true
spring.jpa.properties.hibernate.dialect=org.hibernate.dialect.MySQLDialect
================================================================================
FULL STACK JAVAFX + SPRING BOOT APP
(MYSQL DATABASE VERSION)
Created: 2026-01-29
Version: 1.1 (MySQL Fixed & Verified)
================================================================================
OVERVIEW
This guide shows how to build a complete full-stack application with:
JavaFX Desktop GUI (Frontend)
Spring Boot REST API (Backend)
MySQL Community Server (Persistent Database)
HTTP Client Communication (JSON-based)
This version is FIXED to correctly use MySQL instead of H2.
================================================================================
PREREQUISITES (INSTALL THESE FIRST)
Java 21 JDK
URL: https://www.oracle.com/java/technologies/downloads/
Verify:
java -version
Maven 3.9.12+
URL: https://maven.apache.org/download.cgi
Verify:
mvn -version
MySQL Community Server 8+
URL: https://dev.mysql.com/downloads/mysql/
Port: 3306
Username: root
VS Code (Recommended)
URL: https://code.visualstudio.com/
================================================================================
STEP 1: CREATE DATABASE
Open MySQL Command Line or MySQL Workbench and run:
CREATE DATABASE cc103_db;
================================================================================
STEP 2: CREATE PROJECT STRUCTURE
Command Prompt:
mkdir cc103-app
cd cc103-app
Create folders:
src\main\java\com\example
src\main\java\com\example\controller
src\main\java\com\example\entity
src\main\java\com\example\repository
src\main\java\com\example\service
src\main\resources\com\example\
================================================================================
STEP 3: CREATE pom.xml (MAVEN CONFIGURATION)
File: pom.xml
<modelVersion>4.0.0</modelVersion>

<groupId>com.example</groupId>
<artifactId>cc103</artifactId>
<version>1.0.0</version>

<properties>
    <maven.compiler.source>21</maven.compiler.source>
    <maven.compiler.target>21</maven.compiler.target>
    <project.build.sourceEncoding>UTF-8</project.build.sourceEncoding>
</properties>

<dependencies>

    <!-- JavaFX -->
    <dependency>
        <groupId>org.openjfx</groupId>
        <artifactId>javafx-controls</artifactId>
        <version>23</version>
    </dependency>
    <dependency>
        <groupId>org.openjfx</groupId>
        <artifactId>javafx-fxml</artifactId>
        <version>23</version>
    </dependency>

    <!-- Spring Boot -->
    <dependency>
        <groupId>org.springframework.boot</groupId>
        <artifactId>spring-boot-starter-web</artifactId>
        <version>3.5.0</version>
    </dependency>
    <dependency>
        <groupId>org.springframework.boot</groupId>
        <artifactId>spring-boot-starter-data-jpa</artifactId>
        <version>3.5.0</version>
    </dependency>

    <!-- MySQL Connector -->
    <dependency>
        <groupId>com.mysql</groupId>
        <artifactId>mysql-connector-j</artifactId>
        <version>9.1.0</version>
    </dependency>

    <!-- JSON -->
    <dependency>
        <groupId>com.google.code.gson</groupId>
        <artifactId>gson</artifactId>
        <version>2.10.1</version>
    </dependency>

    <!-- Jakarta Persistence -->
    <dependency>
        <groupId>jakarta.persistence</groupId>
        <artifactId>jakarta.persistence-api</artifactId>
        <version>3.1.0</version>
    </dependency>

</dependencies>

<build>
    <plugins>
        <plugin>
            <groupId>org.apache.maven.plugins</groupId>
            <artifactId>maven-compiler-plugin</artifactId>
            <version>3.13.0</version>
        </plugin>
        <plugin>
            <groupId>org.openjfx</groupId>
            <artifactId>javafx-maven-plugin</artifactId>
            <version>0.0.8</version>
            <configuration>
                <mainClass>com.example.App</mainClass>
            </configuration>
        </plugin>
        <plugin>
            <groupId>org.springframework.boot</groupId>
            <artifactId>spring-boot-maven-plugin</artifactId>
            <version>3.5.0</version>
        </plugin>
    </plugins>
</build>

================================================================================
STEP 4: DATABASE ENTITY
File: src/main/java/com/example/entity/User.java
package com.example.entity;
import jakarta.persistence.*;
@Entity
@Table(name = "users")
public class User {
@Id
@GeneratedValue(strategy = GenerationType.IDENTITY)
private Long id;

@Column(unique = true, nullable = false)
private String username;

private String password;
private String email;

public User() {}

public User(String username, String password, String email) {
    this.username = username;
    this.password = password;
    this.email = email;
}

public Long getId() { return id; }
public String getUsername() { return username; }
public String getPassword() { return password; }
public String getEmail() { return email; }

}
================================================================================
STEP 5: REPOSITORY
File: src/main/java/com/example/repository/UserRepository.java
package com.example.repository;
import org.springframework.data.jpa.repository.JpaRepository;
import com.example.entity.User;
public interface UserRepository extends JpaRepository<User, Long> {
User findByUsername(String username);
boolean existsByUsername(String username);
}
================================================================================
STEP 6: SERVICE
File: src/main/java/com/example/service/UserService.java
package com.example.service;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import com.example.entity.User;
import com.example.repository.UserRepository;
@Service
public class UserService {
@Autowired
private UserRepository userRepository;

public User authenticate(String username, String password) {
    User user = userRepository.findByUsername(username);
    if (user != null && user.getPassword().equals(password)) {
        return user;
    }
    return null;
}

}
================================================================================
STEP 7: REST CONTROLLER
File: src/main/java/com/example/controller/AuthController.java
package com.example.controller;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.web.bind.annotation.;
import com.example.service.UserService;
import com.example.entity.User;
import com.google.gson.;
@RestController
@RequestMapping("/api/auth")
public class AuthController {
@Autowired
private UserService userService;

@PostMapping("/login")
public String login(@RequestBody String body) {
    JsonObject json = JsonParser.parseString(body).getAsJsonObject();
    String username = json.get("username").getAsString();
    String password = json.get("password").getAsString();

    User user = userService.authenticate(username, password);
    JsonObject response = new JsonObject();

    if (user != null) {
        response.addProperty("success", true);
        response.addProperty("userId", user.getId());
        response.addProperty("email", user.getEmail());
    } else {
        response.addProperty("success", false);
        response.addProperty("message", "Invalid credentials");
    }
    return response.toString();
}

}
================================================================================
STEP 8: DATA INITIALIZER
File: src/main/java/com/example/DataInitializer.java
package com.example;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.CommandLineRunner;
import org.springframework.stereotype.Component;
import com.example.entity.User;
import com.example.repository.UserRepository;
@Component
public class DataInitializer implements CommandLineRunner {
@Autowired
private UserRepository userRepository;

@Override
public void run(String... args) {
    if (userRepository.count() == 0) {
        userRepository.save(new User("testuser", "password123", "test@example.com"));
        userRepository.save(new User("admin", "password123", "admin@example.com"));
        System.out.println("✅ MySQL test users inserted");
    }
}

}
================================================================================
STEP 9: SPRING BOOT ENTRY POINT
File: src/main/java/com/example/ServerApplication.java
package com.example;
import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
@SpringBootApplication
public class ServerApplication {
public static void main(String[] args) {
SpringApplication.run(ServerApplication.class, args);
}
}
================================================================================
STEP 10: APPLICATION PROPERTIES (MYSQL)
File: src/main/resources/application.properties
server.port=8080
spring.datasource.url=jdbc:mysql://localhost:3306/cc103_db?useSSL=false&serverTimezone=UTC
spring.datasource.username=root
spring.datasource.password=YOUR_MYSQL_PASSWORD
spring.datasource.driver-class-name=com.mysql.cj.jdbc.Driver
spring.jpa.hibernate.ddl-auto=update
spring.jpa.show-sql=true
spring.jpa.properties.hibernate.dialect=org.hibernate.dialect.MySQLDialect
================================================================================
STEP 11: JAVAFX FXML (UI LAYOUT)
File: src/main/resources/com/example/login.fxml
<Label text="CC103 Login" style="-fx-font-size: 24; -fx-font-weight: bold;"/>

<TextField fx:id="usernameField" promptText="Username" prefWidth="250"/>
<PasswordField fx:id="passwordField" promptText="Password" prefWidth="250"/>

<Label fx:id="errorLabel" style="-fx-text-fill: red;"/>

<Button fx:id="loginButton" text="Login" onAction="#handleLogin" prefWidth="250"/>

================================================================================
STEP 12: JAVAFX CONTROLLER (UI LOGIC)
File: src/main/java/com/example/controller/LoginController.java
package com.example.controller;
import javafx.fxml.FXML;
import javafx.scene.control.*;
import java.net.URI;
import java.net.http.HttpClient;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;
import com.google.gson.Gson;
import com.google.gson.JsonObject;
public class LoginController {
@FXML private TextField usernameField;
@FXML private PasswordField passwordField;
@FXML private Label errorLabel;
@FXML private Button loginButton;

private static final String API_URL = "http://localhost:8080/api/auth/login";
private static final HttpClient httpClient = HttpClient.newHttpClient();
private static final Gson gson = new Gson();

@FXML
private void handleLogin() {
    String username = usernameField.getText().trim();
    String password = passwordField.getText();

    if (username.isEmpty() || password.isEmpty()) {
        errorLabel.setText("Please fill in all fields");
        return;
    }

    loginButton.setDisable(true);
    loginButton.setText("Logging in...");

    new Thread(() -> {
        try {
            JsonObject loginRequest = new JsonObject();
            loginRequest.addProperty("username", username);
            loginRequest.addProperty("password", password);

            HttpRequest request = HttpRequest.newBuilder()
                    .uri(URI.create(API_URL))
                    .header("Content-Type", "application/json")
                    .POST(HttpRequest.BodyPublishers.ofString(loginRequest.toString()))
                    .build();

            HttpResponse<String> response = httpClient.send(request, HttpResponse.BodyHandlers.ofString());

            if (response.statusCode() == 200) {
                JsonObject responseBody = gson.fromJson(response.body(), JsonObject.class);
                if (responseBody.get("success").getAsBoolean()) {
                    javafx.application.Platform.runLater(() -> {
                        errorLabel.setText("✅ Login successful!");
                        errorLabel.setStyle("-fx-text-fill: green;");
                    });
                } else {
                    showError(responseBody.has("message") ? responseBody.get("message").getAsString() : "Invalid credentials");
                }
            } else {
                showError("Server error: " + response.statusCode());
            }
        } catch (Exception e) {
            showError("Connection error: " + e.getMessage());
        } finally {
            javafx.application.Platform.runLater(() -> {
                loginButton.setDisable(false);
                loginButton.setText("Login");
            });
        }
    }).start();
}

private void showError(String message) {
    javafx.application.Platform.runLater(() -> {
        errorLabel.setText(message);
        errorLabel.setStyle("-fx-text-fill: red;");
    });
}

}
================================================================================
STEP 13: JAVAFX APP ENTRY POINT
File: src/main/java/com/example/App.java
package com.example;
import javafx.application.Application;
import javafx.fxml.FXMLLoader;
import javafx.scene.Scene;
import javafx.stage.Stage;
public class App extends Application {
@Override
public void start(Stage stage) throws Exception {
    FXMLLoader loader = new FXMLLoader(App.class.getResource("/com/example/login.fxml"));
    Scene scene = new Scene(loader.load(), 600, 400);
    stage.setTitle("CC103 Login");
    stage.setScene(scene);
    stage.show();
}

public static void main(String[] args) {
    launch();
}

}

STEP 14: RUN THE APPLICATION
Start MySQL Server
mvn clean package
mvn spring-boot:run
mvn javafx:run
================================================================================
LOGIN TEST
Username: testuser
Password: password123
================================================================================
END OF GUIDE

