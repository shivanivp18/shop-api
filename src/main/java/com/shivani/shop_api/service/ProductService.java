
package com.shivani.shop_api.service;

import com.shivani.shop_api.model.Product;
import com.shivani.shop_api.repository.ProductRepository;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;
import org.springframework.web.server.ResponseStatusException;

import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Sort;

import java.util.List;

@Service
public class ProductService {

    private final ProductRepository productRepository;

    public ProductService(ProductRepository productRepository) {
        this.productRepository = productRepository;
    }

    public List<Product> getAllProducts() {
        return productRepository.findAll();
    }

public List<Product> searchProducts(String name) {
    return productRepository.findByNameContainingIgnoreCase(name);
}
public Page<Product> getProductsPaginated(int page, int size, String sortBy) {
    if (page < 0 || size < 1 || size > 100) {
        throw new ResponseStatusException(
            HttpStatus.BAD_REQUEST, "Invalid pagination parameters");
    }

    if (!List.of("id", "name", "price", "stock").contains(sortBy)) {
        throw new ResponseStatusException(
            HttpStatus.BAD_REQUEST, "Invalid sorting field");
    }

    PageRequest pageable = PageRequest.of(
        page,
        size,
        Sort.by(sortBy).ascending()
    );

    return productRepository.findAll(pageable);
}



    public Product getProductById(Long id) {
        return productRepository.findById(id)
            .orElseThrow(() -> new ResponseStatusException(
                HttpStatus.NOT_FOUND, "Product not found"));
    }

    public Product createProduct(Product product) {
        validateProduct(product);
        product.setId(null);
        return productRepository.save(product);
    }

    public Product updateProduct(Long id, Product updatedProduct) {
        validateProduct(updatedProduct);

        Product existingProduct = getProductById(id);
        existingProduct.setName(updatedProduct.getName());
        existingProduct.setPrice(updatedProduct.getPrice());
        existingProduct.setDescription(updatedProduct.getDescription());
        existingProduct.setStock(updatedProduct.getStock());

        return productRepository.save(existingProduct);
    }

    public void deleteProduct(Long id) {
        Product product = getProductById(id);
        productRepository.delete(product);
    }

    private void validateProduct(Product product) {
        if (product == null ||
            product.getName() == null ||
            product.getName().isBlank() ||
            !Double.isFinite(product.getPrice()) ||
            product.getPrice() < 0 ||
            product.getStock() < 0) {

            throw new ResponseStatusException(
                HttpStatus.BAD_REQUEST, "Invalid product information");
        }
    }
}
