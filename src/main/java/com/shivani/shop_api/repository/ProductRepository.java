
package com.shivani.shop_api.repository;

import com.shivani.shop_api.model.Product;
import org.springframework.data.jpa.repository.JpaRepository;

public interface ProductRepository extends JpaRepository<Product, Long> {

}
