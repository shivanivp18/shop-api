
package com.shivani.shop_api.auth;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;

import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.webmvc.test.autoconfigure.AutoConfigureMockMvc;
import org.springframework.http.MediaType;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.test.web.servlet.MockMvc;

import java.util.Map;
import java.util.UUID;

import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertTrue;

import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.delete;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.put;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@SpringBootTest
@AutoConfigureMockMvc
class JwtIntegrationTest {

    @Autowired
    private MockMvc mockMvc;

    @Autowired
    private UserRepository userRepository;

    @Autowired
    private PasswordEncoder passwordEncoder;

    // Manually create ObjectMapper for JSON handling
    private final ObjectMapper objectMapper = new ObjectMapper();

    // =====================================================
    // TEST 1: CUSTOMER REGISTRATION, LOGIN AND AUTHORIZATION
    // =====================================================

    @Test
    void customerRegistrationLoginAndAuthorizationWithRealJwt()
            throws Exception {

        // Step 1: Generate unique CUSTOMER credentials
        String email = "jwt-test-" + UUID.randomUUID() + "@example.com";
        String password = "TestPassword123!";

        // Step 2: Register CUSTOMER
        String registerBody = objectMapper.writeValueAsString(
                Map.of(
                        "fullName", "JWT Integration Test",
                        "email", email,
                        "password", password
                )
        );

        mockMvc.perform(post("/api/auth/register")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(registerBody))
                .andExpect(status().isCreated());

        // Step 3: Log in as CUSTOMER
        String loginBody = objectMapper.writeValueAsString(
                Map.of(
                        "email", email,
                        "password", password
                )
        );

        String loginResponse = mockMvc.perform(
                        post("/api/auth/login")
                                .contentType(MediaType.APPLICATION_JSON)
                                .content(loginBody))
                .andExpect(status().isOk())
                .andReturn()
                .getResponse()
                .getContentAsString();

        // Step 4: Extract real CUSTOMER JWT
        JsonNode customerJson = objectMapper.readTree(loginResponse);

        String customerToken = customerJson
                .path("accessToken")
                .asText();

        assertNotNull(customerToken);
        assertFalse(customerToken.isBlank());

        // Step 5: CUSTOMER cannot access ADMIN endpoints
        mockMvc.perform(get("/api/admin/test")
                        .header("Authorization", "Bearer " + customerToken))
                .andExpect(status().isForbidden());

        // Step 6: CUSTOMER cannot create products
        String productBody = objectMapper.writeValueAsString(
                Map.of(
                        "name", "JWT Test Product",
                        "price", 1000,
                        "description", "JWT authorization test",
                        "stock", 5,
                        "category", "Testing"
                )
        );

        mockMvc.perform(post("/products")
                        .header("Authorization", "Bearer " + customerToken)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(productBody))
                .andExpect(status().isForbidden());

        // Step 7: Public product browsing works without JWT
        mockMvc.perform(get("/products"))
                .andExpect(status().isOk());

        // Step 8: CUSTOMER cannot update products
        mockMvc.perform(put("/products/{id}", 2L)
                        .header("Authorization", "Bearer " + customerToken)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(productBody))
                .andExpect(status().isForbidden());

        // Step 9: CUSTOMER cannot delete products
        mockMvc.perform(delete("/products/{id}", 2L)
                        .header("Authorization", "Bearer " + customerToken))
                .andExpect(status().isForbidden());
    }

    // =====================================================
    // TEST 2: ADMIN AUTHORIZATION AND PRODUCT CRUD OPERATIONS
    // =====================================================

    @Test
    void adminCanAccessProtectedEndpointsWithRealJwt()
            throws Exception {

        // Step 1: Create a unique ADMIN account for testing
        String email = "jwt-admin-" + UUID.randomUUID() + "@example.com";
        String password = "AdminTestPassword123!";

        User admin = new User();

        admin.setFullName("JWT Test Admin");
        admin.setEmail(email);
        admin.setPasswordHash(passwordEncoder.encode(password));
        admin.setRole(Role.ADMIN);

        userRepository.save(admin);

        // Step 2: Log in as ADMIN using the real API
        String loginBody = objectMapper.writeValueAsString(
                Map.of(
                        "email", email,
                        "password", password
                )
        );

        String loginResponse = mockMvc.perform(
                        post("/api/auth/login")
                                .contentType(MediaType.APPLICATION_JSON)
                                .content(loginBody))
                .andExpect(status().isOk())
                .andReturn()
                .getResponse()
                .getContentAsString();

        // Step 3: Extract real ADMIN JWT
        JsonNode adminJson = objectMapper.readTree(loginResponse);

        String adminToken = adminJson
                .path("accessToken")
                .asText();

        assertNotNull(adminToken);
        assertFalse(adminToken.isBlank());

        // Step 4: ADMIN can access protected endpoint
        mockMvc.perform(get("/api/admin/test")
                        .header("Authorization", "Bearer " + adminToken))
                .andExpect(status().isOk());

        // Step 5: ADMIN creates a product
        String productBody = objectMapper.writeValueAsString(
                Map.of(
                        "name", "ADMIN JWT Integration Product",
                        "price", 1500,
                        "description", "Created during ADMIN JWT testing",
                        "stock", 10,
                        "category", "Testing"
                )
        );

        String createdProductResponse = mockMvc.perform(
                        post("/products")
                                .header("Authorization", "Bearer " + adminToken)
                                .contentType(MediaType.APPLICATION_JSON)
                                .content(productBody))
                .andExpect(status().isCreated())
                .andReturn()
                .getResponse()
                .getContentAsString();

        // Step 6: Extract created product ID
        JsonNode createdProductJson = objectMapper
                .readTree(createdProductResponse);

        long productId = createdProductJson
                .path("id")
                .asLong();

        assertTrue(productId > 0, "Product ID must be valid");

        // Step 7: ADMIN updates the product
        String updatedProductBody = objectMapper.writeValueAsString(
                Map.of(
                        "name", "Updated ADMIN JWT Product",
                        "price", 1800,
                        "description", "Updated using real ADMIN JWT",
                        "stock", 15,
                        "category", "Testing"
                )
        );

        mockMvc.perform(put("/products/{id}", productId)
                        .header("Authorization", "Bearer " + adminToken)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(updatedProductBody))
                .andExpect(status().isOk());

        // Step 8: ADMIN deletes the product
        mockMvc.perform(delete("/products/{id}", productId)
                        .header("Authorization", "Bearer " + adminToken))
                .andExpect(status().isNoContent());
    }
}
