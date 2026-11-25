package com.spiceroute.management;

import java.util.List;
import java.util.UUID;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

public interface RestaurantRepository extends JpaRepository<Restaurant, UUID> {
    boolean existsByIdAndOwnerIdAndActiveTrueAndArchivedFalse(UUID id, UUID ownerId);
    List<Restaurant> findAllByOrderByNameAsc();
    List<Restaurant> findByOwnerIdOrderByNameAsc(UUID ownerId);

    @Query("""
        SELECT r FROM Restaurant r
        WHERE r.active = true AND r.archived = false AND (r.ownerId = :userId OR EXISTS (
            SELECT a FROM BranchAssignment a, Branch b
            WHERE a.branchId = b.id AND b.archived = false AND b.restaurant = r AND a.userId = :userId
        )) ORDER BY r.name
        """)
    List<Restaurant> findAccessible(@Param("userId") UUID userId);
}
