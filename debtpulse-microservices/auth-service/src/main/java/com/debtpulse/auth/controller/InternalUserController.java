package com.debtpulse.auth.controller;

import com.debtpulse.common.enums.Role;
import com.debtpulse.auth.dto.response.UserDto;
import com.debtpulse.auth.service.UserService;
import io.swagger.v3.oas.annotations.tags.Tag;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.List;

/**
 * Internal user-lookup API consumed by other microservices via Feign. Not exposed through
 * the public gateway routes; reachable only service-to-service (with propagated identity).
 */
@RestController
@RequestMapping("/api/internal/users")
@Tag(name = "Internal - Users", description = "Service-to-service user lookups (Feign)")
public class InternalUserController {

    private final UserService userService;

    public InternalUserController(UserService userService) {
        this.userService = userService;
    }

    @GetMapping("/{id}")
    public ResponseEntity<UserDto> getById(@PathVariable String id) {
        return ResponseEntity.ok(userService.getById(id));
    }

    @GetMapping("/{id}/exists")
    public ResponseEntity<Boolean> exists(@PathVariable String id) {
        return ResponseEntity.ok(userService.exists(id));
    }

    @GetMapping("/by-role/{role}/first")
    public ResponseEntity<UserDto> firstByRole(@PathVariable Role role) {
        return ResponseEntity.ok(userService.findFirstByRole(role));
    }

    @GetMapping("/by-role/{role}")
    public ResponseEntity<List<UserDto>> byRole(@PathVariable Role role) {
        return ResponseEntity.ok(userService.findByRole(role));
    }

    @GetMapping("/by-role/{role}/active")
    public ResponseEntity<List<UserDto>> activeByRole(@PathVariable Role role,
                                                      @RequestParam(required = false) String branchId) {
        return ResponseEntity.ok(userService.findActiveByRole(role, branchId));
    }
}
