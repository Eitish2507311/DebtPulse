package com.debtpulse.auth.controller;

import com.debtpulse.common.enums.Role;
import com.debtpulse.common.enums.UserStatus;
import com.debtpulse.auth.dto.request.RegisterRequest;
import com.debtpulse.auth.dto.request.UpdateUserRequest;
import com.debtpulse.auth.dto.response.UserDto;
import com.debtpulse.auth.service.UserService;
import com.debtpulse.common.dto.PageResponse;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.data.domain.Sort;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/api/users")
@PreAuthorize("hasRole('ADMIN')")
@Tag(name = "Users", description = "User administration (ADMIN only)")
public class UserController {

    private final UserService userService;

    public UserController(UserService userService) {
        this.userService = userService;
    }

    @GetMapping
    @Operation(summary = "List users (paginated, filterable)")
    public ResponseEntity<PageResponse<UserDto>> list(
            @RequestParam(defaultValue = "0") int page,
            @RequestParam(defaultValue = "20") int size,
            @RequestParam(required = false) Role role,
            @RequestParam(required = false) String branchId,
            @RequestParam(required = false) UserStatus status) {
        Pageable pageable = PageRequest.of(page, size, Sort.by("userId"));
        return ResponseEntity.ok(PageResponse.of(userService.list(role, branchId, status, pageable)));
    }

    @GetMapping("/{id}")
    public ResponseEntity<UserDto> getById(@PathVariable String id) {
        return ResponseEntity.ok(userService.getById(id));
    }

    @PostMapping
    @Operation(summary = "Create a user")
    public ResponseEntity<UserDto> create(@Valid @RequestBody RegisterRequest req) {
        return ResponseEntity.status(HttpStatus.CREATED).body(userService.create(req));
    }

    @PutMapping("/{id}")
    public ResponseEntity<UserDto> update(@PathVariable String id,
                                          @Valid @RequestBody UpdateUserRequest req) {
        return ResponseEntity.ok(userService.update(id, req));
    }

    @PatchMapping("/{id}/status")
    public ResponseEntity<UserDto> updateStatus(@PathVariable String id,
                                                @RequestParam UserStatus status) {
        return ResponseEntity.ok(userService.updateStatus(id, status));
    }

    @DeleteMapping("/{id}")
    @Operation(summary = "Soft-delete a user (marks INACTIVE)")
    public ResponseEntity<Void> delete(@PathVariable String id) {
        userService.delete(id);
        return ResponseEntity.noContent().build();
    }
}
