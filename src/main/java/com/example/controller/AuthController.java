package com.example.controller;

import com.example.entity.User;
import com.example.service.UserService;
import io.jsonwebtoken.Jwts;
import io.jsonwebtoken.SignatureAlgorithm;
import io.jsonwebtoken.security.Keys;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;
import jakarta.validation.constraints.Email;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;

import java.util.Date;
import java.util.HashMap;
import java.util.Map;
import java.util.Optional;

@RestController
@RequestMapping("/api/auth")
@CrossOrigin(origins = "*")
public class AuthController {
    @Autowired
    private UserService userService;
    
    // JWT Secret Key - Use environment variable in production
    private static final byte[] JWT_SECRET = Keys.secretKeyFor(SignatureAlgorithm.HS512).getEncoded();
    private static final long JWT_EXPIRATION = 86400000; // 24 hours

    @PostMapping("/login")
    public ResponseEntity<?> login(@RequestBody LoginRequest request) {
        try {
            // Validate input
            if (request.getUsername() == null || request.getUsername().isBlank()) {
                return ResponseEntity.badRequest().body(createErrorResponse("Username is required"));
            }
            if (request.getPassword() == null || request.getPassword().isBlank()) {
                return ResponseEntity.badRequest().body(createErrorResponse("Password is required"));
            }

            // Authenticate user
            Optional<User> user = userService.authenticateUser(request.getUsername(), request.getPassword());
            if (user.isPresent()) {
                // Generate JWT Token
                String token = generateJwtToken(user.get());
                
                Map<String, Object> response = new HashMap<>();
                response.put("success", true);
                response.put("message", "Login successful");
                response.put("token", token);
                response.put("userId", user.get().getId());
                response.put("username", user.get().getUsername());
                response.put("email", user.get().getEmail());
                return ResponseEntity.ok(response);
            } else {
                return ResponseEntity.status(HttpStatus.UNAUTHORIZED)
                        .body(createErrorResponse("Invalid username or password"));
            }
        } catch (Exception e) {
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR)
                    .body(createErrorResponse("Login failed: " + e.getMessage()));
        }
    }

    @PostMapping("/register")
    public ResponseEntity<?> register(@RequestBody RegisterRequest request) {
        try {
            // Validate input
            if (request.getUsername() == null || request.getUsername().isBlank()) {
                return ResponseEntity.badRequest().body(createErrorResponse("Username is required"));
            }
            if (request.getUsername().length() < 3) {
                return ResponseEntity.badRequest().body(createErrorResponse("Username must be at least 3 characters"));
            }
            if (request.getPassword() == null || request.getPassword().isBlank()) {
                return ResponseEntity.badRequest().body(createErrorResponse("Password is required"));
            }
            if (request.getPassword().length() < 6) {
                return ResponseEntity.badRequest().body(createErrorResponse("Password must be at least 6 characters"));
            }
            if (request.getEmail() == null || request.getEmail().isBlank()) {
                return ResponseEntity.badRequest().body(createErrorResponse("Email is required"));
            }
            if (!request.getEmail().contains("@")) {
                return ResponseEntity.badRequest().body(createErrorResponse("Invalid email format"));
            }

            // Register user
            User user = userService.registerUser(request.getUsername(), request.getPassword(), request.getEmail());
            Map<String, Object> response = new HashMap<>();
            response.put("success", true);
            response.put("message", "User registered successfully");
            response.put("userId", user.getId());
            response.put("username", user.getUsername());
            response.put("email", user.getEmail());
            return ResponseEntity.status(HttpStatus.CREATED).body(response);
        } catch (RuntimeException e) {
            return ResponseEntity.badRequest().body(createErrorResponse(e.getMessage()));
        } catch (Exception e) {
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR)
                    .body(createErrorResponse("Registration failed: " + e.getMessage()));
        }
    }

    @GetMapping("/verify/{token}")
    public ResponseEntity<?> verifyToken(@PathVariable String token) {
        try {
            // Verify token and extract username
            String username = verifyJwtToken(token);
            if (username != null) {
                Optional<User> user = userService.findByUsername(username);
                if (user.isPresent()) {
                    Map<String, Object> response = new HashMap<>();
                    response.put("valid", true);
                    response.put("username", user.get().getUsername());
                    return ResponseEntity.ok(response);
                }
            }
            return ResponseEntity.status(HttpStatus.UNAUTHORIZED)
                    .body(createErrorResponse("Invalid token"));
        } catch (Exception e) {
            return ResponseEntity.status(HttpStatus.UNAUTHORIZED)
                    .body(createErrorResponse("Token verification failed"));
        }
    }

    @GetMapping("/user/{id}")
    public ResponseEntity<?> getUser(@PathVariable Long id) {
        try {
            Optional<User> user = userService.findById(id);
            if (user.isPresent()) {
                Map<String, Object> response = new HashMap<>();
                response.put("id", user.get().getId());
                response.put("username", user.get().getUsername());
                response.put("email", user.get().getEmail());
                return ResponseEntity.ok(response);
            }
            return ResponseEntity.notFound().build();
        } catch (Exception e) {
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR)
                    .body(createErrorResponse("Failed to fetch user"));
        }
    }

    // Helper methods
    private String generateJwtToken(User user) {
        return Jwts.builder()
                .setSubject(user.getUsername())
                .setIssuedAt(new Date())
                .setExpiration(new Date(System.currentTimeMillis() + JWT_EXPIRATION))
                .signWith(Keys.hmacShaKeyFor(JWT_SECRET), SignatureAlgorithm.HS512)
                .compact();
    }

    private String verifyJwtToken(String token) {
        try {
            return Jwts.parser()
                    .verifyWith(Keys.hmacShaKeyFor(JWT_SECRET))
                    .build()
                    .parseSignedClaims(token)
                    .getPayload()
                    .getSubject();
        } catch (Exception e) {
            return null;
        }
    }

    private Map<String, Object> createErrorResponse(String message) {
        Map<String, Object> response = new HashMap<>();
        response.put("success", false);
        response.put("message", message);
        return response;
    }

    // Request classes
    public static class LoginRequest {
        private String username;
        private String password;

        public LoginRequest() {}

        public String getUsername() { return username; }
        public void setUsername(String username) { this.username = username; }
        public String getPassword() { return password; }
        public void setPassword(String password) { this.password = password; }
    }

    public static class RegisterRequest {
        private String username;
        private String password;
        private String email;

        public RegisterRequest() {}

        public String getUsername() { return username; }
        public void setUsername(String username) { this.username = username; }
        public String getPassword() { return password; }
        public void setPassword(String password) { this.password = password; }
        public String getEmail() { return email; }
        public void setEmail(String email) { this.email = email; }
    }
}
