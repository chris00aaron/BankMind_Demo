package com.naal.bankmind.repository.Login;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import com.naal.bankmind.entity.Login.PasswordResetRequest;
import com.naal.bankmind.entity.Login.User;

import java.util.List;
import java.util.Optional;

@Repository
public interface PasswordResetRequestRepository extends JpaRepository<PasswordResetRequest, Long> {

    List<PasswordResetRequest> findByStatusOrderByRequestedAtDesc(PasswordResetRequest.Status status);

    Optional<PasswordResetRequest> findByUserAndStatus(User user, PasswordResetRequest.Status status);

    boolean existsByUserAndStatus(User user, PasswordResetRequest.Status status);
}
