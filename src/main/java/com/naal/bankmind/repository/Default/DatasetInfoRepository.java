package com.naal.bankmind.repository.Default;

import com.naal.bankmind.entity.Default.DatasetInfo;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

@Repository
public interface DatasetInfoRepository extends JpaRepository<DatasetInfo, Long> {
}
