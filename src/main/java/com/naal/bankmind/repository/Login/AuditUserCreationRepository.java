package com.naal.bankmind.repository.Login;

import com.naal.bankmind.entity.Login.AuditUserCreation;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;

@Repository
public interface AuditUserCreationRepository extends JpaRepository<AuditUserCreation, Long> {

    List<AuditUserCreation> findAllByOrderByCreatedAtDesc();
}
