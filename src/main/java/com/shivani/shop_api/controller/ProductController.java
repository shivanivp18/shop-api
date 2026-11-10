
package com.shivani.shop_api.controller;

import com.shivani.shop_api.model.Product;
import com.shivani.shop_api.service.ProductService;

import org.springframework.http.HttpStatus;
import org.springframework.web.bind.annotation.*;
import org.springframework.data.domain.Page;
import java.util.List;

@RestController
@RequestMapping("/products")
public class ProductController {

    private final ProductService productService;

    public ProductController(ProductService productService) {
        this.productService = productService;
    }

    // GET: Retrieve all products
    @GetMapping
    public List<Product> getAllProducts() {
        return productService.getAllProducts();
    }

@GetMapping("/search")
public List<Product> searchProducts(@RequestParam String name) {
    return productService.searchProducts(name);
}

@GetMapping("/paginated")
public Page<Product> getProductsPaginated(
        @RequestParam(defaultValue = "0") int page,
        @RequestParam(defaultValue = "5") int size,
        @RequestParam(defaultValue = "id") String sortBy) {

    return productService.getProductsPaginated(page, size, sortBy);
}


    // GET: Retrieve product by ID
    @GetMapping("/{id}")
    public Product getProductById(@PathVariable Long id) {
        return productService.getProductById(id);
    }

    // POST: Create a new product
    @PostMapping
    @ResponseStatus(HttpStatus.CREATED)
    public Product createProduct(@RequestBody Product product) {
        return productService.createProduct(product);
    }

    // PUT: Update an existing product
    @PutMapping("/{id}")
    public Product updateProduct(
            @PathVariable Long id,
            @RequestBody Product product) {

        return productService.updateProduct(id, product);
    }

    // DELETE: Remove a product
    @DeleteMapping("/{id}")
    @ResponseStatus(HttpStatus.NO_CONTENT)
    public void deleteProduct(@PathVariable Long id) {
        productService.deleteProduct(id);
    }
}
