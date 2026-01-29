package com.example;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.CommandLineRunner;
import org.springframework.stereotype.Component;

import com.example.entity.User;
import com.example.entity.Product;
import com.example.repository.UserRepository;
import com.example.repository.ProductRepository;

@Component
public class DataInitializer implements CommandLineRunner {
    
    @Autowired
    private UserRepository userRepository;
    
    @Autowired
    private ProductRepository productRepository;
    
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
        
        // Create sample products if they don't exist
        if (productRepository.findByName("Laptop") == null) {
            productRepository.save(new Product("Laptop", "High-performance laptop with 16GB RAM", 999.99, 10));
        }
        if (productRepository.findByName("Mouse") == null) {
            productRepository.save(new Product("Mouse", "Wireless mouse with precision tracking", 29.99, 50));
        }
        if (productRepository.findByName("Keyboard") == null) {
            productRepository.save(new Product("Keyboard", "Mechanical keyboard with RGB lighting", 79.99, 25));
        }
        if (productRepository.findByName("Monitor") == null) {
            productRepository.save(new Product("Monitor", "4K UHD 27-inch display", 349.99, 15));
        }
        if (productRepository.findByName("Headphones") == null) {
            productRepository.save(new Product("Headphones", "Noise-cancelling wireless headphones", 149.99, 30));
        }
        
        System.out.println("\n✅ Test users initialized!");
        System.out.println("📝 Available login credentials:");
        System.out.println("   Username: testuser | Password: password123");
        System.out.println("   Username: admin   | Password: password123");
        System.out.println("   Username: john    | Password: password123");
        System.out.println("   Username: jane    | Password: password123");
        System.out.println("\n📦 Sample products created:");
        System.out.println("   • Laptop - $999.99");
        System.out.println("   • Mouse - $29.99");
        System.out.println("   • Keyboard - $79.99");
        System.out.println("   • Monitor - $349.99");
        System.out.println("   • Headphones - $149.99");
        System.out.println("\n🌐 Server running on http://localhost:8080");
        System.out.println("📚 API Endpoints:");
        System.out.println("   POST   /api/auth/login       - Login user");
        System.out.println("   POST   /api/auth/register    - Register new user");
        System.out.println("   GET    /api/auth/user/{id}   - Get user info");
        System.out.println("   GET    /api/products         - Get all products");
        System.out.println("   POST   /api/products         - Add product");
        System.out.println("   GET    /api/products/{id}    - Get product");
        System.out.println("   PUT    /api/products/{id}    - Update product");
        System.out.println("   DELETE /api/products/{id}    - Delete product\n");
    }
}
