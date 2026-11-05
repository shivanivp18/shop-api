
package com.shivani.shop_api.controller;

import com.shivani.shop_api.model.Product;
import com.shivani.shop_api.repository.ProductRepository;

import org.springframework.http.HttpStatus;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.server.ResponseStatusException;


import java.util.List;

@RestController
@RequestMapping("/products")
public class ProductController {

    // Repository for future MySQL operations
    private final ProductRepository productRepository;

    // Constructor injection
    public ProductController(ProductRepository productRepository) {
        this.productRepository = productRepository;
    }

    private long nextId = 3L;

    // API 1: Get all products from MySQL
    @GetMapping
    public List<Product> getAllProducts() {
        return productRepository.findAll();
    }

    
    // API 2: Get product by ID from MySQL
    @GetMapping("/{id}")
    public Product getProductById(@PathVariable Long id) {

        return productRepository.findById(id)
                .orElseThrow(() -> new ResponseStatusException(
                        HttpStatus.NOT_FOUND,
                        "Product not found"
                ));
    }


    // API 3: Create a product
    
    // API 3: Create a product in MySQL
    @PostMapping
    @ResponseStatus(HttpStatus.CREATED)
    public synchronized Product createProduct(
            @RequestBody Product product) {

        if (product.getName() == null
                || product.getName().isBlank()
                || !Double.isFinite(product.getPrice())
                || product.getPrice() < 0
                || product.getStock() < 0) {

            throw new ResponseStatusException(
                HttpStatus.BAD_REQUEST,
                "Invalid product information"
            );
        }

        // Let MySQL generate the product ID
        product.setId(null);

        // Save the product to MySQL
        return productRepository.save(product);
    }

    // API 4: Update an existing product in MySQL
    @PutMapping("/{id}")
    public Product updateProduct(
            @PathVariable Long id,
            @RequestBody Product updatedProduct) {

        // Validate incoming product information
        if (updatedProduct.getName() == null
                || updatedProduct.getName().isBlank()
                || !Double.isFinite(updatedProduct.getPrice())
                || updatedProduct.getPrice() < 0
                || updatedProduct.getStock() < 0) {

            throw new ResponseStatusException(
                HttpStatus.BAD_REQUEST,
                "Invalid product information"
            );
        }

        // Find the existing product in MySQL
        Product existingProduct = productRepository.findById(id)
                .orElseThrow(() -> new ResponseStatusException(
                        HttpStatus.NOT_FOUND,
                        "Product not found"
                ));

        // Update the existing product's fields
        existingProduct.setName(updatedProduct.getName());
        existingProduct.setPrice(updatedProduct.getPrice());
        existingProduct.setDescription(updatedProduct.getDescription());
        existingProduct.setStock(updatedProduct.getStock());

        // Save the updated product to MySQL
        return productRepository.save(existingProduct);
    }


    // API 5: Delete a product by ID
    
@DeleteMapping("/{id}")
@ResponseStatus(HttpStatus.NO_CONTENT)
public void deleteProduct(@PathVariable Long id) {

    Product existingProduct = productRepository.findById(id)
        .orElseThrow(() -> new ResponseStatusException(
            HttpStatus.NOT_FOUND,
            "Product not found"
        ));

    productRepository.delete(existingProduct);
}
}