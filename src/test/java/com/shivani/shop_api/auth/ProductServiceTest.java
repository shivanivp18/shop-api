
package com.shivani.shop_api.service;

import java.util.List;
import java.util.Optional;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertSame;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import static org.mockito.ArgumentMatchers.any;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.verifyNoInteractions;
import static org.mockito.Mockito.when;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageImpl;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Sort;
import org.springframework.http.HttpStatus;
import org.springframework.web.server.ResponseStatusException;

import com.shivani.shop_api.model.Product;
import com.shivani.shop_api.repository.ProductRepository;



@ExtendWith(MockitoExtension.class)
class ProductServiceTest {

    @Mock
    private ProductRepository productRepository;

    @InjectMocks
    private ProductService productService;

    // TEST 1: Retrieve an existing product
    @Test
    void getProductById_shouldReturnProduct_whenProductExists() {

        // Arrange
        Long productId = 1L;

        Product product = mock(Product.class);

        when(productRepository.findById(productId))
                .thenReturn(Optional.of(product));

        // Act
        Product result = productService.getProductById(productId);

        // Assert
        assertNotNull(result);
        assertSame(product, result);

        verify(productRepository).findById(productId);
    }

    // TEST 2: Retrieve a nonexistent product
    @Test
    void getProductById_shouldThrow404_whenProductDoesNotExist() {

        // Arrange
        Long productId = 999L;

        when(productRepository.findById(productId))
                .thenReturn(Optional.empty());

        // Act
        ResponseStatusException exception = assertThrows(
                ResponseStatusException.class,
                () -> productService.getProductById(productId)
        );

        // Assert
        assertEquals(
                HttpStatus.NOT_FOUND,
                exception.getStatusCode()
        );

        verify(productRepository).findById(productId);
    }

    // TEST 3: Create a valid product
    @Test
    void createProduct_shouldSaveValidProduct() {

        // Arrange
        Product product = mock(Product.class);

        when(product.getName()).thenReturn("Dell Laptop");
        when(product.getPrice()).thenReturn(55000.0);
        when(product.getStock()).thenReturn(10);

        when(productRepository.save(product))
                .thenReturn(product);

        // Act
        Product result = productService.createProduct(product);

        // Assert
        assertNotNull(result);
        assertSame(product, result);

        // Verify that the product ID is reset
        verify(product).setId(null);

        // Verify that the repository saves the product
        verify(productRepository).save(product);
    }
    // TEST 4: Reject a product with a negative price
    @Test
    void createProduct_shouldThrow400_whenPriceIsNegative() {

        // Arrange: Create an invalid product
        Product product = mock(Product.class);

        when(product.getName()).thenReturn("Dell Laptop");
        when(product.getPrice()).thenReturn(-500.0);

        // Act: Attempt to create the invalid product
        ResponseStatusException exception = assertThrows(
                ResponseStatusException.class,
                () -> productService.createProduct(product)
        );

        // Assert: Verify HTTP 400 Bad Request
        assertEquals(
                HttpStatus.BAD_REQUEST,
                exception.getStatusCode()
        );

        // Invalid products must never be saved
        verify(productRepository, never()).save(any(Product.class));
    }

    // TEST 5: Reject a product with negative stock
    @Test
    void createProduct_shouldThrow400_whenStockIsNegative() {

        // Arrange: Create a product with invalid stock
        Product product = mock(Product.class);

        when(product.getName()).thenReturn("Dell Laptop");
        when(product.getPrice()).thenReturn(55000.0);
        when(product.getStock()).thenReturn(-10);

        // Act: Attempt to create the invalid product
        ResponseStatusException exception = assertThrows(
                ResponseStatusException.class,
                () -> productService.createProduct(product)
        );

        // Assert: Verify HTTP 400 Bad Request
        assertEquals(
                HttpStatus.BAD_REQUEST,
                exception.getStatusCode()
        );

        // Invalid products must never be saved
        verify(productRepository, never()).save(any(Product.class));
    }

    // TEST 6: Reject a product with a blank name
    @Test
    void createProduct_shouldThrow400_whenNameIsBlank() {

        // Arrange: Product has an invalid blank name
        Product product = mock(Product.class);

        when(product.getName()).thenReturn("   ");

        // Act: Attempt to create the invalid product
        ResponseStatusException exception = assertThrows(
                ResponseStatusException.class,
                () -> productService.createProduct(product)
        );

        // Assert: Verify HTTP 400 Bad Request
        assertEquals(
                HttpStatus.BAD_REQUEST,
                exception.getStatusCode()
        );

        // Verify invalid product is never saved
        verify(productRepository, never()).save(any(Product.class));
    }

    // TEST 7: Update an existing product
    @Test
    void updateProduct_shouldSaveUpdatedProduct_whenProductExists() {

        // Arrange: Existing product ID
        Long productId = 1L;

        // Existing product stored in the repository
        Product existingProduct = mock(Product.class);

        // New product information supplied by the user
        Product updatedProduct = mock(Product.class);

        when(updatedProduct.getName()).thenReturn("HP Laptop");
        when(updatedProduct.getPrice()).thenReturn(60000.0);
        when(updatedProduct.getStock()).thenReturn(15);

        when(productRepository.findById(productId))
                .thenReturn(Optional.of(existingProduct));

        when(productRepository.save(existingProduct))
                .thenReturn(existingProduct);

        // Act: Update the product
        Product result = productService.updateProduct(
                productId,
                updatedProduct
        );

        // Assert: Verify the returned product
        assertNotNull(result);
        assertSame(existingProduct, result);

        // Verify the new values are copied
        verify(existingProduct).setName("HP Laptop");
        verify(existingProduct).setPrice(60000.0);
        verify(existingProduct).setStock(15);

        // Verify the repository saves the updated product
        verify(productRepository).save(existingProduct);
    }

    // TEST 8: Reject updating a nonexistent product
    @Test
    void updateProduct_shouldThrow404_whenProductDoesNotExist() {

        // Arrange
        Long productId = 999L;

        Product updatedProduct = mock(Product.class);

        when(updatedProduct.getName()).thenReturn("HP Laptop");
        when(updatedProduct.getPrice()).thenReturn(60000.0);
        when(updatedProduct.getStock()).thenReturn(15);

        when(productRepository.findById(productId))
                .thenReturn(Optional.empty());

        // Act
        ResponseStatusException exception = assertThrows(
                ResponseStatusException.class,
                () -> productService.updateProduct(
                        productId,
                        updatedProduct
                )
        );

        // Assert
        assertEquals(
                HttpStatus.NOT_FOUND,
                exception.getStatusCode()
        );

        verify(productRepository).findById(productId);

        // A nonexistent product must not be saved
        verify(productRepository, never()).save(any(Product.class));
    }
     
    // TEST 9: Delete an existing product
    @Test
    void deleteProduct_shouldDeleteProduct_whenProductExists() {

        // Arrange: Product ID to delete
        Long productId = 1L;

        // Simulate an existing product
        Product existingProduct = mock(Product.class);

        when(productRepository.findById(productId))
                .thenReturn(Optional.of(existingProduct));

        // Act: Delete the product
        productService.deleteProduct(productId);

        // Assert: Verify the product was found
        verify(productRepository).findById(productId);

        // Verify the repository deleted the correct product
        verify(productRepository).delete(existingProduct);
    }

    // TEST 10: Reject deleting a nonexistent product
    @Test
    void deleteProduct_shouldThrow404_whenProductDoesNotExist() {

        // Arrange: Product ID does not exist
        Long productId = 999L;

        when(productRepository.findById(productId))
                .thenReturn(Optional.empty());

        // Act: Attempt to delete the missing product
        ResponseStatusException exception = assertThrows(
                ResponseStatusException.class,
                () -> productService.deleteProduct(productId)
        );

        // Assert: Verify HTTP 404 Not Found
        assertEquals(
                HttpStatus.NOT_FOUND,
                exception.getStatusCode()
        );

        // Verify the repository searched for the product
        verify(productRepository).findById(productId);

        // Verify that nothing was deleted
        verify(productRepository, never()).delete(any(Product.class));
    }
    
    // TEST 11: Search products by name
    @Test
    void searchProducts_shouldReturnMatchingProducts() {

        // Arrange: Search keyword
        String searchKeyword = "laptop";

        // Create two mock products
        Product product1 = mock(Product.class);
        Product product2 = mock(Product.class);

        // Simulate repository search results
        List<Product> matchingProducts = List.of(product1, product2);

        when(productRepository.findByNameContainingIgnoreCase(searchKeyword))
                .thenReturn(matchingProducts);

        // Act: Search for products
        List<Product> result = productService.searchProducts(searchKeyword);

        // Assert: Verify the returned results
        assertNotNull(result);
        assertEquals(2, result.size());
        assertEquals(matchingProducts, result);

        // Verify the repository search method was called
        verify(productRepository)
                .findByNameContainingIgnoreCase(searchKeyword);
    }

    // TEST 12: Filter products by category
    @Test
    void getProductsByCategory_shouldReturnMatchingProducts() {

        // Arrange: Choose a category
        String category = "Electronics";

        Product product1 = mock(Product.class);
        Product product2 = mock(Product.class);

        List<Product> matchingProducts = List.of(product1, product2);

        when(productRepository.findByCategoryIgnoreCase(category))
                .thenReturn(matchingProducts);

        // Act: Retrieve products by category
        List<Product> result =
                productService.getProductsByCategory(category);

        // Assert: Verify matching products are returned
        assertNotNull(result);
        assertEquals(2, result.size());
        assertEquals(matchingProducts, result);

        // Verify the repository method was called
        verify(productRepository)
                .findByCategoryIgnoreCase(category);
    }

    // TEST 13: Retrieve paginated products
    @Test
    void getProductsPaginated_shouldReturnCorrectPage() {

        // Arrange: Request first page, 2 products per page
        int page = 0;
        int size = 2;
        String sortBy = "name";

        Product product1 = mock(Product.class);
        Product product2 = mock(Product.class);

        PageRequest pageable = PageRequest.of(
                page,
                size,
                Sort.by(sortBy).ascending()
        );

        Page<Product> expectedPage = new PageImpl<>(
                List.of(product1, product2),
                pageable,
                5
        );

        when(productRepository.findAll(pageable))
                .thenReturn(expectedPage);

        // Act: Retrieve paginated products
        Page<Product> result =
                productService.getProductsPaginated(page, size, sortBy);

        // Assert: Verify pagination results
        assertNotNull(result);
        assertEquals(2, result.getContent().size());
        assertEquals(5, result.getTotalElements());
        assertEquals(3, result.getTotalPages());
        assertEquals(0, result.getNumber());

        // Verify correct pagination and sorting parameters
        verify(productRepository).findAll(pageable);
    }

    // TEST 14: Reject negative page number
    @Test
    void pagination_shouldRejectNegativePage() {

        ResponseStatusException exception = assertThrows(
                ResponseStatusException.class,
                () -> productService.getProductsPaginated(-1, 10, "name")
        );

        assertEquals(HttpStatus.BAD_REQUEST, exception.getStatusCode());
        verifyNoInteractions(productRepository);
    }

    // TEST 15: Reject zero page size
    @Test
    void pagination_shouldRejectZeroPageSize() {

        ResponseStatusException exception = assertThrows(
                ResponseStatusException.class,
                () -> productService.getProductsPaginated(0, 0, "name")
        );

        assertEquals(HttpStatus.BAD_REQUEST, exception.getStatusCode());
        verifyNoInteractions(productRepository);
    }

    // TEST 16: Reject negative page size
    @Test
    void pagination_shouldRejectNegativePageSize() {

        ResponseStatusException exception = assertThrows(
                ResponseStatusException.class,
                () -> productService.getProductsPaginated(0, -5, "name")
        );

        assertEquals(HttpStatus.BAD_REQUEST, exception.getStatusCode());
        verifyNoInteractions(productRepository);
    }

    // TEST 17: Reject page size greater than 100
    @Test
    void pagination_shouldRejectPageSizeOver100() {

        ResponseStatusException exception = assertThrows(
                ResponseStatusException.class,
                () -> productService.getProductsPaginated(0, 101, "name")
        );

        assertEquals(HttpStatus.BAD_REQUEST, exception.getStatusCode());
        verifyNoInteractions(productRepository);
    }

    // TEST 18: Reject unsupported sorting field
    @Test
    void pagination_shouldRejectInvalidSortField() {

        ResponseStatusException exception = assertThrows(
                ResponseStatusException.class,
                () -> productService.getProductsPaginated(
                        0, 10, "invalidField"
                )
        );

        assertEquals(HttpStatus.BAD_REQUEST, exception.getStatusCode());
        verifyNoInteractions(productRepository);
    }

    // TEST 19: Allow maximum permitted page size
    @Test
    void pagination_shouldAllowPageSize100() {

        PageRequest pageable = PageRequest.of(
                0, 100, Sort.by("id").ascending()
        );

        Page<Product> expectedPage = new PageImpl<>(
                List.of(), pageable, 0
        );

        when(productRepository.findAll(pageable))
                .thenReturn(expectedPage);

        Page<Product> result =
                productService.getProductsPaginated(0, 100, "id");

        assertNotNull(result);
        assertTrue(result.isEmpty());

        verify(productRepository).findAll(pageable);
    }

    // TEST 20: Verify sorting by price
    @Test
    void pagination_shouldSortByPriceAscending() {

        PageRequest pageable = PageRequest.of(
                0, 10, Sort.by("price").ascending()
        );

        Page<Product> expectedPage = new PageImpl<>(
                List.of(), pageable, 0
        );

        when(productRepository.findAll(pageable))
                .thenReturn(expectedPage);

        Page<Product> result =
                productService.getProductsPaginated(0, 10, "price");

        assertNotNull(result);
        assertEquals(
                Sort.by("price").ascending(),
                result.getSort()
        );

        verify(productRepository).findAll(pageable);
    }

    // TEST 21: Retrieve all products
    @Test
    void getAllProducts_shouldReturnProducts() {

        Product product1 = mock(Product.class);
        Product product2 = mock(Product.class);

        List<Product> products = List.of(product1, product2);

        when(productRepository.findAll()).thenReturn(products);

        List<Product> result = productService.getAllProducts();

        assertEquals(2, result.size());
        assertEquals(products, result);

        verify(productRepository).findAll();
    }

    // TEST 22: Return empty list when no products exist
    @Test
    void getAllProducts_shouldReturnEmptyList() {

        when(productRepository.findAll())
                .thenReturn(List.of());

        List<Product> result = productService.getAllProducts();

        assertNotNull(result);
        assertTrue(result.isEmpty());

        verify(productRepository).findAll();
    }

    // TEST 23: Return empty search results
    @Test
    void searchProducts_shouldReturnEmptyListWhenNoMatch() {

        String keyword = "unknown-product";

        when(productRepository.findByNameContainingIgnoreCase(keyword))
                .thenReturn(List.of());

        List<Product> result =
                productService.searchProducts(keyword);

        assertNotNull(result);
        assertTrue(result.isEmpty());

        verify(productRepository)
                .findByNameContainingIgnoreCase(keyword);
    }

    // TEST 24: Return empty category results
    @Test
    void getProductsByCategory_shouldReturnEmptyListWhenNoMatch() {

        String category = "UnknownCategory";

        when(productRepository.findByCategoryIgnoreCase(category))
                .thenReturn(List.of());

        List<Product> result =
                productService.getProductsByCategory(category);

        assertNotNull(result);
        assertTrue(result.isEmpty());

        verify(productRepository)
                .findByCategoryIgnoreCase(category);
    }

    // TEST 25: Reject null product
    @Test
    void createProduct_shouldRejectNullProduct() {

        ResponseStatusException exception = assertThrows(
                ResponseStatusException.class,
                () -> productService.createProduct(null)
        );

        assertEquals(HttpStatus.BAD_REQUEST, exception.getStatusCode());

        verifyNoInteractions(productRepository);
    }

    // TEST 26: Reject null product name
    @Test
    void createProduct_shouldRejectNullName() {

        Product product = mock(Product.class);

        ResponseStatusException exception = assertThrows(
                ResponseStatusException.class,
                () -> productService.createProduct(product)
        );

        assertEquals(HttpStatus.BAD_REQUEST, exception.getStatusCode());

        verify(productRepository, never()).save(any(Product.class));
    }

    // TEST 27: Reject NaN price
    @Test
    void createProduct_shouldRejectNaNPrice() {

        Product product = mock(Product.class);

        when(product.getName()).thenReturn("Laptop");
        when(product.getPrice()).thenReturn(Double.NaN);

        ResponseStatusException exception = assertThrows(
                ResponseStatusException.class,
                () -> productService.createProduct(product)
        );

        assertEquals(HttpStatus.BAD_REQUEST, exception.getStatusCode());

        verify(productRepository, never()).save(any(Product.class));
    }

    // TEST 28: Reject infinite price
    @Test
    void createProduct_shouldRejectInfinitePrice() {

        Product product = mock(Product.class);

        when(product.getName()).thenReturn("Laptop");
        when(product.getPrice())
                .thenReturn(Double.POSITIVE_INFINITY);

        ResponseStatusException exception = assertThrows(
                ResponseStatusException.class,
                () -> productService.createProduct(product)
        );

        assertEquals(HttpStatus.BAD_REQUEST, exception.getStatusCode());

        verify(productRepository, never()).save(any(Product.class));
    }

    // TEST 29: Allow product with zero price
    @Test
    void createProduct_shouldAllowZeroPrice() {

        Product product = mock(Product.class);

        when(product.getName()).thenReturn("Free Sample");

        when(productRepository.save(product))
                .thenReturn(product);

        Product result = productService.createProduct(product);

        assertSame(product, result);
        verify(product).setId(null);
        verify(productRepository).save(product);
    }

    // TEST 30: Reject invalid product update
    @Test
    void updateProduct_shouldRejectInvalidProduct() {

        Product updatedProduct = mock(Product.class);

        when(updatedProduct.getName()).thenReturn("Laptop");
        when(updatedProduct.getPrice()).thenReturn(-100.0);

        ResponseStatusException exception = assertThrows(
                ResponseStatusException.class,
                () -> productService.updateProduct(1L, updatedProduct)
        );

        assertEquals(HttpStatus.BAD_REQUEST, exception.getStatusCode());

        verifyNoInteractions(productRepository);
    }

    // TEST 31: Get products within price range
    @Test
    void priceRange_shouldReturnMatchingProducts() {

        Product product1 = mock(Product.class);
        Product product2 = mock(Product.class);

        List<Product> products = List.of(product1, product2);

        when(productRepository.findByPriceBetween(1000.0, 5000.0))
                .thenReturn(products);

        List<Product> result =
                productService.getProductsByPriceRange(1000.0, 5000.0);

        assertEquals(2, result.size());
        assertEquals(products, result);

        verify(productRepository)
                .findByPriceBetween(1000.0, 5000.0);
    }

    // TEST 32: Reject negative minimum price range
    @Test
    void priceRange_shouldRejectNegativeMinimum() {

        assertThrows(
                IllegalArgumentException.class,
                () -> productService.getProductsByPriceRange(-1.0, 5000.0)
        );

        verifyNoInteractions(productRepository);
    }

    // TEST 33: Reject minimum greater than maximum price
    @Test
    void priceRange_shouldRejectMinimumGreaterThanMaximum() {

        assertThrows(
                IllegalArgumentException.class,
                () -> productService.getProductsByPriceRange(5000.0, 1000.0)
        );

        verifyNoInteractions(productRepository);
    }

    // TEST 34: Empty price range result
    @Test
    void priceRange_shouldReturnEmptyListWhenNoMatch() {

        when(productRepository.findByPriceBetween(100.0, 200.0))
                .thenReturn(List.of());

        List<Product> result =
                productService.getProductsByPriceRange(100.0, 200.0);

        assertTrue(result.isEmpty());

        verify(productRepository)
                .findByPriceBetween(100.0, 200.0);
    }

    }
    

    

