package com.spiceroute.security;

import java.time.Instant;
import java.util.Optional;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

public interface SessionRepository extends JpaRepository<UserSession, String> {

    @Query("""
        SELECT s.user FROM UserSession s
        WHERE s.tokenHash = :tokenHash AND s.expiresAt > :now AND s.user.active = true AND s.user.ownerSuspended = false
        """)
    Optional<UserAccount> findActiveUser(@Param("tokenHash") String tokenHash, @Param("now") Instant now);

    @Modifying
    @Query("DELETE FROM UserSession s WHERE s.user.id = :userId")
    int revokeAll(@Param("userId") java.util.UUID userId);

    @Modifying
    @Query("DELETE FROM UserSession s WHERE s.expiresAt <= :now")
    int deleteExpired(@Param("now") Instant now);
}
