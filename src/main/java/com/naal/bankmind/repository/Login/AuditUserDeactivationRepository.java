package com.naal.bankmind.repository.Login;

import com.naal.bankmind.entity.Login.AuditUserDeactivation;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;

@Repository
public interface AuditUserDeactivationRepository extends JpaRepository<AuditUserDeactivation, Long> {

    List<AuditUserDeactivation> findAllByOrderByDeactivatedAtDesc();
}
