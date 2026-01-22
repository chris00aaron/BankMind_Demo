package com.naal.bankmind.repository.Shared;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import com.naal.bankmind.entity.Login.Role;

import java.util.Optional;

@Repository
public interface RoleRepository extends JpaRepository<Role, Short> {

    Optional<Role> findByCodRole(String codRole);

    Optional<Role> findByName(String name);
}
