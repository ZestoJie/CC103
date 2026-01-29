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
    public void run(String... args) throws Exception {
        // Create test users if they don't exist
        if (!userRepository.existsByUsername("testuser")) {
            userRepository.save(new User("testuser", "password123", "test@example.com"));
        }
        if (!userRepository.existsByUsername("admin")) {
            userRepository.save(new User("admin", "password123", "admin@example.com"));
        }
        if (!userRepository.existsByUsername("john")) {
            userRepository.save(new User("john", "password123", "john@example.com"));
        }
        if (!userRepository.existsByUsername("jane")) {
            userRepository.save(new User("jane", "password123", "jane@example.com"));
        }
        
        System.out.println("\n✅ Test users initialized!");
        System.out.println("📝 Available login credentials:");
        System.out.println("   Username: testuser | Password: password123");
        System.out.println("   Username: admin   | Password: password123");
        System.out.println("   Username: john    | Password: password123");
        System.out.println("   Username: jane    | Password: password123");
        System.out.println("🌐 Server running on http://localhost:8080\n");
    }
}
