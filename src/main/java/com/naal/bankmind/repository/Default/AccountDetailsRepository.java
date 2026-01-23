package com.naal.bankmind.repository.Default;

import com.naal.bankmind.entity.AccountDetails;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

@Repository
public interface AccountDetailsRepository extends JpaRepository<AccountDetails, Long> {
}
