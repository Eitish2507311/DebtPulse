package com.debtpulse.auth.controller;

import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.web.bind.annotation.*;

import java.util.Map;

/**
 * Developer utility to BCrypt-encode a raw string (useful when seeding users manually).
 * Public — kept identical to the monolith's {@code /api/util/hash} endpoint.
 */
@RestController
@RequestMapping("/api/util")
@Tag(name = "Utility", description = "Developer helpers")
public class HashController {

    private final PasswordEncoder encoder;

    public HashController(PasswordEncoder encoder) {
        this.encoder = encoder;
    }

    @GetMapping("/hash")
    @Operation(summary = "BCrypt-encode a raw string")
    public Map<String, String> hash(@RequestParam String raw) {
        return Map.of("raw", raw, "bcrypt", encoder.encode(raw));
    }
}
