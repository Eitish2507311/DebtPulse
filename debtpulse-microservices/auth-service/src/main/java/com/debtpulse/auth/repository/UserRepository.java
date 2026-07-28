package com.debtpulse.auth.repository;

import com.debtpulse.common.enums.Role;
import com.debtpulse.common.enums.UserStatus;
import com.debtpulse.auth.entity.User;
import org.springframework.data.jpa.repository.JpaSpecificationExecutor;
import org.springframework.data.repository.CrudRepository;
import org.springframework.data.repository.PagingAndSortingRepository;

import java.util.List;
import java.util.Optional;

/**
 * User persistence. Uses {@link PagingAndSortingRepository} + {@link CrudRepository} for CRUD,
 * paging and derived finders, plus {@link JpaSpecificationExecutor} so the list endpoint can
 * apply dynamic Specification-based filtering.
 */
public interface UserRepository extends PagingAndSortingRepository<User, String>,
        CrudRepository<User, String>, JpaSpecificationExecutor<User> {

    Optional<User> findByEmail(String email);

    Optional<User> findByResetToken(String resetToken);

    List<User> findByRole(Role role);

    Optional<User> findFirstByRole(Role role);

    List<User> findByRoleAndStatus(Role role, UserStatus status);

    List<User> findByRoleAndStatusAndBranchId(Role role, UserStatus status, String branchId);

    boolean existsByEmail(String email);
}
