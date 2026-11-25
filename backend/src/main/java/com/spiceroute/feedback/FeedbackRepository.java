package com.spiceroute.feedback;

import com.spiceroute.analytics.BranchPerformance;
import java.time.Instant;
import java.util.List;
import java.util.UUID;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

public interface FeedbackRepository extends JpaRepository<CustomerFeedback, UUID> {

    @Query("""
        SELECT new com.spiceroute.feedback.FeedbackView(
            f.id, f.rating, f.comment, f.customerName, f.phone,
            f.language, f.createdAt, t.label, f.reviewStatus, f.internalNote, f.version
        ) FROM CustomerFeedback f JOIN f.diningTable t
        WHERE t.branch.id = :branchId AND f.createdAt >= :from AND f.createdAt < :to
            AND (:rating IS NULL OR f.rating = :rating)
            AND (:tableId IS NULL OR t.id = :tableId)
        ORDER BY f.createdAt DESC, f.id
        """)
    List<FeedbackView> findForBranch(
        @Param("branchId") UUID branchId,
        @Param("from") Instant from,
        @Param("to") Instant to,
        @Param("rating") Integer rating,
        @Param("tableId") UUID tableId
    );

    @Query("""
        SELECT new com.spiceroute.analytics.BranchPerformance(b.id, b.name, COUNT(f.id), AVG(f.rating))
        FROM Branch b JOIN b.restaurant r
        LEFT JOIN DiningTable t ON t.branch = b
        LEFT JOIN CustomerFeedback f ON f.diningTable = t
        WHERE r.active = true AND r.archived = false AND b.archived = false AND (r.ownerId = :userId OR EXISTS (
            SELECT a FROM BranchAssignment a WHERE a.branchId = b.id AND a.userId = :userId
        )) GROUP BY b.id, b.name ORDER BY b.name
        """)
    List<BranchPerformance> compareBranches(@Param("userId") UUID userId);
}
