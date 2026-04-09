package com.naal.bankmind.repository.Churn;

import com.naal.bankmind.entity.Churn.ChurnSampleBatch;
import com.naal.bankmind.entity.Churn.ChurnSampleEntry;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.util.List;

@Repository
public interface ChurnSampleEntryRepository extends JpaRepository<ChurnSampleEntry, Long> {

    @Query("SELECT e FROM ChurnSampleEntry e JOIN FETCH e.customer WHERE e.batch = :batch")
    List<ChurnSampleEntry> findByBatchWithCustomer(@Param("batch") ChurnSampleBatch batch);

    void deleteByBatch(ChurnSampleBatch batch);
}
