package com.example.inventory.web.api;

import java.time.Instant;
import java.util.LinkedHashMap;
import java.util.Map;

public final class ApiErrorBody {

    private ApiErrorBody() {
    }

    public static Map<String, Object> of(int status, String error, String message, String path) {
        Map<String, Object> body = new LinkedHashMap<>();
        body.put("timestamp", Instant.now().toString());
        body.put("status", status);
        body.put("error", error);
        body.put("message", message);
        body.put("path", path);
        return body;
    }
}
