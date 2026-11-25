package com.spiceroute.management;

import java.util.List;
import java.util.UUID;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

public interface BranchRepository extends JpaRepository<Branch, UUID> {
    @Query("""
        SELECT b FROM Branch b JOIN FETCH b.restaurant r
        WHERE r.active = true AND r.archived = false AND b.archived = false AND (r.ownerId = :userId OR EXISTS (
            SELECT a FROM BranchAssignment a
            WHERE a.branchId = b.id AND a.userId = :userId
        )) ORDER BY b.name
        """)
    List<Branch> findAccessible(@Param("userId") UUID userId);

    @Query("""
        SELECT COUNT(b) FROM Branch b JOIN b.restaurant r
        WHERE b.id = :branchId AND r.active = true AND r.archived = false AND b.archived = false AND (r.ownerId = :userId OR EXISTS (
            SELECT a FROM BranchAssignment a
            WHERE a.branchId = b.id AND a.userId = :userId
        ))
        """)
    long countAccessible(@Param("branchId") UUID branchId, @Param("userId") UUID userId);

    List<Branch> findByRestaurant_OwnerIdOrderByNameAsc(UUID ownerId);

    @Query("SELECT b.id FROM Branch b WHERE b.restaurant.ownerId = :ownerId")
    List<UUID> findOwnedBranchIds(@Param("ownerId") UUID ownerId);
}
