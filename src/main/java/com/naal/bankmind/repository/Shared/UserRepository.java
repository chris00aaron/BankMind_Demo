package com.naal.bankmind.repository.Shared;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import com.naal.bankmind.entity.Login.User;

import java.util.List;
import java.util.Optional;

@Repository
public interface UserRepository extends JpaRepository<User, Long> {

    List<User> findByEnableTrue();

    Optional<User> findByEmail(String email);

    Optional<User> findByDni(String dni);

    boolean existsByEmail(String email);

    boolean existsByDni(String dni);
}
