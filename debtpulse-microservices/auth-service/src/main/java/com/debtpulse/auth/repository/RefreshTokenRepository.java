package com.debtpulse.auth.repository;

import com.debtpulse.auth.entity.RefreshToken;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;
import java.util.Optional;

public interface RefreshTokenRepository extends JpaRepository<RefreshToken, String> {

    Optional<RefreshToken> findByTokenHash(String tokenHash);

    List<RefreshToken> findBySessionId(String sessionId);

    List<RefreshToken> findByUserIdAndRevokedFalse(String userId);
}
