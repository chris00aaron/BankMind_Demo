package com.naal.bankmind.repository.atm;

import java.util.List;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import com.naal.bankmind.entity.atm.Atm;



@Repository
public interface AtmRepository extends JpaRepository<Atm, Long> {
    List<Atm> findByActiveTrue();
}
