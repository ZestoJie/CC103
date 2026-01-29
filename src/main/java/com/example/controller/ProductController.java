package com.example.controller;

import com.example.entity.Product;
import com.example.service.ProductService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Optional;

@RestController
@RequestMapping("/api/products")
@CrossOrigin(origins = "*")
public class ProductController {
    
    @Autowired
    private ProductService productService;
    
    @PostMapping
    public ResponseEntity<?> addProduct(@RequestBody ProductRequest request) {
        try {
            if (request.getName() == null || request.getName().isBlank()) {
                return ResponseEntity.badRequest().body(createErrorResponse("Product name is required"));
            }
            if (request.getPrice() == null || request.getPrice() <= 0) {
                return ResponseEntity.badRequest().body(createErrorResponse("Price must be greater than 0"));
            }
            if (request.getStock() == null || request.getStock() < 0) {
                return ResponseEntity.badRequest().body(createErrorResponse("Stock cannot be negative"));
            }
            
            Product product = productService.addProduct(
                    request.getName(),
                    request.getDescription(),
                    request.getPrice(),
                    request.getStock()
            );
            
            Map<String, Object> response = new HashMap<>();
            response.put("success", true);
            response.put("message", "Product added successfully");
            response.put("product", product);
            return ResponseEntity.status(HttpStatus.CREATED).body(response);
        } catch (RuntimeException e) {
            return ResponseEntity.badRequest().body(createErrorResponse(e.getMessage()));
        } catch (Exception e) {
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR)
                    .body(createErrorResponse("Failed to add product"));
        }
    }
    
    @GetMapping
    public ResponseEntity<?> getAllProducts() {
        try {
            List<Product> products = productService.getAllProducts();
            return ResponseEntity.ok(products);
        } catch (Exception e) {
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR)
                    .body(createErrorResponse("Failed to fetch products"));
        }
    }
    
    @GetMapping("/{id}")
    public ResponseEntity<?> getProduct(@PathVariable Long id) {
        try {
            Optional<Product> product = productService.getProductById(id);
            if (product.isPresent()) {
                return ResponseEntity.ok(product.get());
            }
            return ResponseEntity.notFound().build();
        } catch (Exception e) {
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR)
                    .body(createErrorResponse("Failed to fetch product"));
        }
    }
    
    @PutMapping("/{id}")
    public ResponseEntity<?> updateProduct(@PathVariable Long id, @RequestBody ProductRequest request) {
        try {
            if (request.getName() == null || request.getName().isBlank()) {
                return ResponseEntity.badRequest().body(createErrorResponse("Product name is required"));
            }
            if (request.getPrice() == null || request.getPrice() <= 0) {
                return ResponseEntity.badRequest().body(createErrorResponse("Price must be greater than 0"));
            }
            
            Product product = productService.updateProduct(
                    id,
                    request.getName(),
                    request.getDescription(),
                    request.getPrice(),
                    request.getStock()
            );
            
            Map<String, Object> response = new HashMap<>();
            response.put("success", true);
            response.put("message", "Product updated successfully");
            response.put("product", product);
            return ResponseEntity.ok(response);
        } catch (RuntimeException e) {
            return ResponseEntity.badRequest().body(createErrorResponse(e.getMessage()));
        } catch (Exception e) {
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR)
                    .body(createErrorResponse("Failed to update product"));
        }
    }
    
    @DeleteMapping("/{id}")
    public ResponseEntity<?> deleteProduct(@PathVariable Long id) {
        try {
            productService.deleteProduct(id);
            Map<String, Object> response = new HashMap<>();
            response.put("success", true);
            response.put("message", "Product deleted successfully");
            return ResponseEntity.ok(response);
        } catch (RuntimeException e) {
            return ResponseEntity.badRequest().body(createErrorResponse(e.getMessage()));
        } catch (Exception e) {
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR)
                    .body(createErrorResponse("Failed to delete product"));
        }
    }
    
    private Map<String, Object> createErrorResponse(String message) {
        Map<String, Object> response = new HashMap<>();
        response.put("success", false);
        response.put("message", message);
        return response;
    }
    
    public static class ProductRequest {
        private String name;
        private String description;
        private Double price;
        private Integer stock;
        
        public ProductRequest() {}
        
        public String getName() { return name; }
        public void setName(String name) { this.name = name; }
        
        public String getDescription() { return description; }
        public void setDescription(String description) { this.description = description; }
        
        public Double getPrice() { return price; }
        public void setPrice(Double price) { this.price = price; }
        
        public Integer getStock() { return stock; }
        public void setStock(Integer stock) { this.stock = stock; }
    }
}
