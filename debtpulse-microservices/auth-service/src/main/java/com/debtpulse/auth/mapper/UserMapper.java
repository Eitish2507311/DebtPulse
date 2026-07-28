package com.debtpulse.auth.mapper;

import com.debtpulse.auth.dto.response.UserDto;
import com.debtpulse.auth.entity.User;
import org.springframework.stereotype.Component;

/** Converts between the {@link User} entity and its safe {@link UserDto} projection. */
@Component
public class UserMapper {

    public UserDto toDto(User user) {
        if (user == null) return null;
        return new UserDto(
                user.getUserId(),
                user.getFullName(),
                user.getEmail(),
                user.getPhone(),
                user.getRole() == null ? null : user.getRole().name(),
                user.getBranchId(),
                user.getStatus() == null ? null : user.getStatus().name(),
                user.getCreatedAt()
        );
    }
}
