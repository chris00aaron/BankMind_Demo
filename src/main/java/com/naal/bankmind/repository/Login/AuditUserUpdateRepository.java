package com.naal.bankmind.repository.Login;

import com.naal.bankmind.entity.Login.AuditUserUpdate;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;

@Repository
public interface AuditUserUpdateRepository extends JpaRepository<AuditUserUpdate, Long> {

    List<AuditUserUpdate> findAllByOrderByUpdatedAtDesc();
}
