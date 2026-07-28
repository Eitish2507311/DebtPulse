package com.debtpulse.auth.service;

import com.debtpulse.common.enums.Role;
import com.debtpulse.common.enums.UserStatus;
import com.debtpulse.auth.dto.request.RegisterRequest;
import com.debtpulse.auth.dto.request.UpdateUserRequest;
import com.debtpulse.auth.dto.response.UserDto;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;

import java.util.List;

/** User administration (admin only) plus internal lookups consumed by other services. */
public interface UserService {

    Page<UserDto> list(Role role, String branchId, UserStatus status, Pageable pageable);

    UserDto getById(String id);

    UserDto create(RegisterRequest request);

    UserDto update(String id, UpdateUserRequest request);

    UserDto updateStatus(String id, UserStatus status);

    void delete(String id);

    // ---- internal (Feign) lookups ----
    UserDto findFirstByRole(Role role);

    List<UserDto> findByRole(Role role);

    List<UserDto> findActiveByRole(Role role, String branchId);

    boolean exists(String id);
}
