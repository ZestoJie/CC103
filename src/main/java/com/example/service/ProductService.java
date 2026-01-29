package com.example.service;

import com.example.entity.Product;
import com.example.repository.ProductRepository;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import java.util.List;
import java.util.Optional;

@Service
public class ProductService {
    
    @Autowired
    private ProductRepository productRepository;
    
    public Product addProduct(String name, String description, Double price, Integer stock) {
        if (productRepository.findByName(name) != null) {
            throw new RuntimeException("Product already exists");
        }
        Product product = new Product(name, description, price, stock);
        return productRepository.save(product);
    }
    
    public Product updateProduct(Long id, String name, String description, Double price, Integer stock) {
        Optional<Product> product = productRepository.findById(id);
        if (product.isPresent()) {
            Product p = product.get();
            p.setName(name);
            p.setDescription(description);
            p.setPrice(price);
            p.setStock(stock);
            return productRepository.save(p);
        }
        throw new RuntimeException("Product not found");
    }
    
    public void deleteProduct(Long id) {
        if (!productRepository.existsById(id)) {
            throw new RuntimeException("Product not found");
        }
        productRepository.deleteById(id);
    }
    
    public List<Product> getAllProducts() {
        return productRepository.findAll();
    }
    
    public Optional<Product> getProductById(Long id) {
        return productRepository.findById(id);
    }
    
    public Product getProductByName(String name) {
        return productRepository.findByName(name);
    }
}
