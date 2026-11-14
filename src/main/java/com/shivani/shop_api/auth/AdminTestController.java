package com.shivani.shop_api.auth;

import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
public class AdminTestController {

    @GetMapping("/api/admin/test")
    public String testAdminAccess() {
        return "ADMIN ACCESS GRANTED";
    }
}