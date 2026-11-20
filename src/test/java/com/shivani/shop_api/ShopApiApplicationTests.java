
package com.shivani.shop_api;

import java.util.List;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import static org.mockito.ArgumentMatchers.any;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;
import org.mockito.junit.jupiter.MockitoExtension;

import com.shivani.shop_api.model.Product;
import com.shivani.shop_api.repository.ProductRepository;
import com.shivani.shop_api.service.ProductService;

@ExtendWith(MockitoExtension.class)
class ShopApiApplicationTests {

    @Mock
    private ProductRepository productRepository;

    @InjectMocks
    private ProductService productService;

    private Product testProduct;

    @BeforeEach
    void setUp() {
        testProduct = new Product();
        testProduct.setId(1L);
        testProduct.setName("Dell Laptop");
        testProduct.setPrice(55000);
        testProduct.setDescription("16GB RAM");
        testProduct.setStock(10);
    }

    // TEST 1: Retrieve all products
    @Test
    void shouldGetAllProducts() {

        when(productRepository.findAll())
                .thenReturn(List.of(testProduct));

        List<Product> products = productService.getAllProducts();

        assertEquals(1, products.size());
        assertEquals("Dell Laptop", products.get(0).getName());

        verify(productRepository).findAll();
    }

    // TEST 2: Create a product
    @Test
    void shouldCreateProduct() {

        when(productRepository.save(any(Product.class)))
                .thenReturn(testProduct);

        Product createdProduct =
                productService.createProduct(testProduct);

        assertNotNull(createdProduct);
        assertEquals("Dell Laptop", createdProduct.getName());

        verify(productRepository).save(any(Product.class));
    }

    // TEST 3: Search products by name
    @Test
    void shouldSearchProducts() {

        when(productRepository.findByNameContainingIgnoreCase("Dell"))
                .thenReturn(List.of(testProduct));

        List<Product> products =
                productService.searchProducts("Dell");

        assertEquals(1, products.size());
        assertEquals("Dell Laptop", products.get(0).getName());

        verify(productRepository)
                .findByNameContainingIgnoreCase("Dell");
    }
}
