package com.naal.bankmind.repository;

import com.naal.bankmind.entity.Role;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.Optional;

@Repository
public interface RoleRepository extends JpaRepository<Role, Short> {

    Optional<Role> findByCodRole(String codRole);

    Optional<Role> findByName(String name);
}
