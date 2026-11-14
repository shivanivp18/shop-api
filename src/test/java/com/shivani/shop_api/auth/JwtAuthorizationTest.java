
package com.shivani.shop_api.auth;

import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.webmvc.test.autoconfigure.AutoConfigureMockMvc;
import org.springframework.security.test.context.support.WithMockUser;
import org.springframework.test.web.servlet.MockMvc;

import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@SpringBootTest
@AutoConfigureMockMvc
class JwtAuthorizationTest {

    @Autowired
    private MockMvc mockMvc;

    // Test 1: No JWT should return HTTP 401
    @Test
    void adminEndpointWithoutJwtShouldReturn401() throws Exception {

        mockMvc.perform(get("/api/admin/test"))
                .andExpect(status().isUnauthorized());
    }

    // Test 2: CUSTOMER should not access ADMIN endpoints
    @Test
    @WithMockUser(username = "rahul@example.com", roles = {"CUSTOMER"})
    void customerCannotAccessAdminEndpoint() throws Exception {

        mockMvc.perform(get("/api/admin/test"))
                .andExpect(status().isForbidden());
    }

// Test 3: ADMIN should access ADMIN endpoints
@Test
@WithMockUser(username = "shopadmin@example.com", roles = {"ADMIN"})
void adminCanAccessAdminEndpoint() throws Exception {

    mockMvc.perform(get("/api/admin/test"))
            .andExpect(status().isOk());
}
}