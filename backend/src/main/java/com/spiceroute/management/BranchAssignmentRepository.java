package com.spiceroute.management;

import java.util.List;
import java.util.UUID;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

public interface BranchAssignmentRepository extends JpaRepository<BranchAssignment, BranchAssignment.Key> {
    @Query("SELECT a.branchId FROM BranchAssignment a WHERE a.userId = :userId")
    List<UUID> findBranchIds(@Param("userId") UUID userId);
}
