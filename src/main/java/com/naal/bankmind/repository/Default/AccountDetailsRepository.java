package com.naal.bankmind.repository.Default;

import com.naal.bankmind.entity.AccountDetails;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.util.List;

@Repository
public interface AccountDetailsRepository extends JpaRepository<AccountDetails, Long> {

    /**
     * Encuentra todas las cuentas de un cliente.
     */
    List<AccountDetails> findByCustomer_IdCustomer(Long idCustomer);

    /**
     * Obtiene múltiples cuentas con sus clientes en una sola query.
     * Usa JOIN FETCH para evitar N+1 queries.
     */
    @Query("SELECT ad FROM AccountDetails ad JOIN FETCH ad.customer c " +
            "LEFT JOIN FETCH c.education LEFT JOIN FETCH c.marriage LEFT JOIN FETCH c.gender " +
            "WHERE ad.recordId IN :recordIds")
    List<AccountDetails> findAllWithCustomerByRecordIds(@Param("recordIds") List<Long> recordIds);
}
