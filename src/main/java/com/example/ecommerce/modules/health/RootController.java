package com.example.ecommerce.modules.health;

import com.example.ecommerce.common.api.ApiResponse;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RestController;

import java.util.Map;

@RestController
public class RootController {

    @GetMapping("/")
    public ApiResponse<Map<String, String>> root() {
        return ApiResponse.ok(Map.of(
                "service", "ecommerce-practice",
                "health", "/api/v1/health",
                "actuator", "/actuator/health"
        ));
    }
}
