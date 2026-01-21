package com.naal.bankmind.repository;

import com.naal.bankmind.entity.OtpVerification;
import com.naal.bankmind.entity.User;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;
import org.springframework.stereotype.Repository;

import java.time.LocalDateTime;
import java.util.Optional;

@Repository
public interface OtpVerificationRepository extends JpaRepository<OtpVerification, Long> {

    Optional<OtpVerification> findByMfaTokenAndVerifiedFalse(String mfaToken);

    Optional<OtpVerification> findByUserAndVerifiedFalseAndExpiresAtAfter(User user, LocalDateTime now);

    @Modifying
    @Query("DELETE FROM OtpVerification o WHERE o.expiresAt < :now")
    void deleteExpiredOtps(LocalDateTime now);

    @Modifying
    @Query("UPDATE OtpVerification o SET o.verified = true WHERE o.user = :user AND o.verified = false")
    void invalidateAllUserOtps(User user);
}
