package com.spiceroute.security;

import java.util.List;
import java.util.Optional;
import java.util.UUID;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

public interface UserRepository extends JpaRepository<UserAccount, UUID> {
    Optional<UserAccount> findByEmailAndActiveTrue(String email);
    @org.springframework.data.jpa.repository.Lock(jakarta.persistence.LockModeType.PESSIMISTIC_WRITE)
    @Query("SELECT u FROM UserAccount u WHERE u.id = :id")
    Optional<UserAccount> lockById(@Param("id") UUID id);

    boolean existsByEmail(String email);
    List<UserAccount> findAllByOrderByNameAsc();

    @Query("""
        SELECT u FROM UserAccount u
        WHERE u.id IN (
            SELECT assignment.userId FROM BranchAssignment assignment
            WHERE assignment.branchId = :branchId
        )
        ORDER BY u.name
        """)
    List<UserAccount> findBranchManagers(@Param("branchId") UUID branchId);
}
