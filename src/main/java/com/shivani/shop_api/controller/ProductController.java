
package com.shivani.shop_api.controller;

import com.shivani.shop_api.model.Product;

import org.springframework.http.HttpStatus;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.ResponseStatus;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.web.server.ResponseStatusException;

import java.util.Arrays;
import java.util.List;

@RestController
@RequestMapping("/products")
public class ProductController {

    private final List<Product> products = Arrays.asList(
        new Product(
            1L,
            "Dell Laptop",
            55000.00,
            "16GB RAM, 512GB SSD",
            10
        ),
        new Product(
            2L,
            "Samsung Smartphone",
            25000.00,
            "128GB Storage",
            15
        )
    );

    // API 1: Get all products
    @GetMapping
    public List<Product> getAllProducts() {
        return products;
    }

    // API 2: Get one product by ID
    @GetMapping("/{id}")
    public Product getProductById(@PathVariable Long id) {

        for (Product product : products) {

            if (product.getId().equals(id)) {
                return product;
            }
        }

        throw new ResponseStatusException(
            HttpStatus.NOT_FOUND,
            "Product not found"
        );
    }
}
