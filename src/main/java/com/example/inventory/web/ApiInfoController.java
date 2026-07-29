package com.example.inventory.web;

import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
public class ApiInfoController {

    @GetMapping("/api/info")
    public String apiInfo() {
        return "Inventory Management API is ready";
    }
}
