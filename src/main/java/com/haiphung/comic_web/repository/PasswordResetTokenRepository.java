package com.haiphung.comic_web.repository;

import com.haiphung.comic_web.entity.PasswordResetToken;
import com.haiphung.comic_web.entity.User;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.time.LocalDateTime;
import java.util.Optional;

@Repository
public interface PasswordResetTokenRepository extends JpaRepository<PasswordResetToken, Integer> {
    Optional<PasswordResetToken> findByTokenString(String tokenString);

    void deleteByExpiryDateBefore(LocalDateTime now);
}