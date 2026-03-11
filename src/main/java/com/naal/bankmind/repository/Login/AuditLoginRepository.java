package com.naal.bankmind.repository.Login;

import com.naal.bankmind.entity.Login.AuditLogin;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;

@Repository
public interface AuditLoginRepository extends JpaRepository<AuditLogin, Long> {

    List<AuditLogin> findAllByOrderByLoginAtDesc();

    List<AuditLogin> findByEmailContainingIgnoreCaseOrderByLoginAtDesc(String email);
}
