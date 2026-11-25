package com.spiceroute.management;

import java.util.List;
import java.util.Optional;
import java.util.UUID;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

public interface DiningTableRepository extends JpaRepository<DiningTable, UUID> {
    List<DiningTable> findByBranch_IdOrderByLabelAsc(UUID branchId);
    List<DiningTable> findByBranch_IdAndActiveTrueOrderByLabelAsc(UUID branchId);

    @Query("SELECT t.branch.id FROM DiningTable t WHERE t.id = :id")
    Optional<UUID> findBranchId(@Param("id") UUID id);

    @Query("""
        SELECT t FROM DiningTable t JOIN FETCH t.branch b JOIN FETCH b.restaurant r
        WHERE t.token = :token AND t.active = true AND b.archived = false AND r.active = true AND r.archived = false
        """)
    Optional<DiningTable> findAvailableByToken(@Param("token") UUID token);
}
