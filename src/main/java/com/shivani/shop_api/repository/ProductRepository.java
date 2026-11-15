
package com.shivani.shop_api.repository;

import com.shivani.shop_api.model.Product;
import org.springframework.data.jpa.repository.JpaRepository;
import java.util.List;

public interface ProductRepository extends JpaRepository<Product, Long> {

    // Search products by name
    List<Product> findByNameContainingIgnoreCase(String name);

    // Filter products by category
     List<Product> findByCategoryIgnoreCase(String category);

// Find products within a price range
List<Product> findByPriceBetween(double minPrice, double maxPrice);
}
