
package com.shivani.shop_api.service;

import com.shivani.shop_api.model.Product;
import com.shivani.shop_api.repository.ProductRepository;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;
import org.springframework.web.server.ResponseStatusException;

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
