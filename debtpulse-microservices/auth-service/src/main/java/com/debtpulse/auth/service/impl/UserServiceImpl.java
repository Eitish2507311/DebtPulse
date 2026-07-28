package com.debtpulse.auth.service.impl;

import com.debtpulse.common.enums.Role;
import com.debtpulse.common.enums.UserStatus;
import com.debtpulse.auth.dto.request.RegisterRequest;
import com.debtpulse.auth.dto.request.UpdateUserRequest;
import com.debtpulse.auth.dto.response.UserDto;
import com.debtpulse.auth.entity.User;
import com.debtpulse.auth.mapper.UserMapper;
import com.debtpulse.auth.repository.UserRepository;
import com.debtpulse.auth.repository.UserSpecifications;
import com.debtpulse.auth.service.UserService;
import com.debtpulse.auth.exception.BusinessRuleException;
import com.debtpulse.auth.exception.ResourceNotFoundException;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;

import java.util.List;

@Service
public class UserServiceImpl implements UserService {

    private static final Logger log = LoggerFactory.getLogger(UserServiceImpl.class);

    private final UserRepository userRepo;
    private final UserMapper mapper;
    private final PasswordEncoder encoder;

    public UserServiceImpl(UserRepository userRepo, UserMapper mapper,
                           PasswordEncoder encoder) {
        this.userRepo = userRepo;
        this.mapper = mapper;
        this.encoder = encoder;
    }

    @Override
    public Page<UserDto> list(Role role, String branchId, UserStatus status, Pageable pageable) {
        return userRepo.findAll(UserSpecifications.withFilters(role, branchId, status), pageable)
                .map(mapper::toDto);
    }

    @Override
    public UserDto getById(String id) {
        return mapper.toDto(find(id));
    }

    @Override
    public UserDto create(RegisterRequest req) {
        if (userRepo.existsByEmail(req.email())) {
            throw new BusinessRuleException("Email already registered: " + req.email(), "DUPLICATE_EMAIL");
        }
        User user = User.builder()
                .fullName(req.fullName())
                .email(req.email())
                .phone(req.phone())
                .passwordHash(encoder.encode(req.password()))
                .role(req.role())
                .branchId(req.branchId() != null ? req.branchId() : "B01")
                .status(UserStatus.ACTIVE)
                .build();
        User saved = userRepo.save(user);
        log.info("User created id={} role={}", saved.getUserId(), saved.getRole());
        return mapper.toDto(saved);
    }

    @Override
    public UserDto update(String id, UpdateUserRequest req) {
        User user = find(id);
        if (req.fullName() != null) user.setFullName(req.fullName());
        if (req.email() != null) user.setEmail(req.email());
        if (req.phone() != null) user.setPhone(req.phone());
        if (req.role() != null) user.setRole(req.role());
        if (req.branchId() != null) user.setBranchId(req.branchId());
        return mapper.toDto(userRepo.save(user));
    }

    @Override
    public UserDto updateStatus(String id, UserStatus status) {
        User user = find(id);
        user.setStatus(status);
        return mapper.toDto(userRepo.save(user));
    }

    @Override
    public void delete(String id) {
        // Soft-delete: never physically remove staff — mark INACTIVE for audit integrity.
        User user = find(id);
        user.setStatus(UserStatus.INACTIVE);
        userRepo.save(user);
        log.info("User soft-deleted (INACTIVE) id={}", id);
    }

    @Override
    public UserDto findFirstByRole(Role role) {
        return userRepo.findFirstByRole(role).map(mapper::toDto).orElse(null);
    }

    @Override
    public List<UserDto> findByRole(Role role) {
        return userRepo.findByRole(role).stream().map(mapper::toDto).toList();
    }

    @Override
    public List<UserDto> findActiveByRole(Role role, String branchId) {
        List<User> users = (branchId == null || branchId.isBlank())
                ? userRepo.findByRoleAndStatus(role, UserStatus.ACTIVE)
                : userRepo.findByRoleAndStatusAndBranchId(role, UserStatus.ACTIVE, branchId);
        return users.stream().map(mapper::toDto).toList();
    }

    @Override
    public boolean exists(String id) {
        return userRepo.existsById(id);
    }

    private User find(String id) {
        return userRepo.findById(id)
                .orElseThrow(() -> new ResourceNotFoundException("User not found: " + id));
    }
}
