package com.naal.bankmind.repository.Churn;

import com.naal.bankmind.entity.RetentionSegmentDef;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

@Repository
public interface RetentionSegmentDefRepository extends JpaRepository<RetentionSegmentDef, Integer> {
}
