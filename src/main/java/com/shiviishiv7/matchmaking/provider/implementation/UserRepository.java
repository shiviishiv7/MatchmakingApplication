package com.shiviishiv7.matchmaking.provider.implementation;


import com.shiviishiv7.matchmaking.common.enums.UserStatus;
import com.shiviishiv7.matchmaking.provider.model.User;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;
import java.util.UUID;

@Repository
public interface UserRepository extends JpaRepository<User, UUID> {

    Optional<User> findByCognitoSub(String cognitoSub);

    Optional<User> findByEmail(String email);

    boolean existsByEmail(String email);

    // Fetch all users in the pool — the matching scheduler uses this
    @Query("SELECT u FROM User u WHERE u.status = :status AND u.isActive = true")
    List<User> findAllByStatus(UserStatus status);

    // Fetch pool users excluding a specific user (used to find candidates)
    @Query("""
        SELECT u FROM User u
        WHERE u.status = 'IN_POOL'
        AND u.isActive = true
        AND u.id != :excludeUserId
        AND u.id NOT IN (
            SELECT m.userB.id FROM Match m WHERE m.userA.id = :excludeUserId
            UNION
            SELECT m.userA.id FROM Match m WHERE m.userB.id = :excludeUserId
        )
    """)
    List<User> findCandidatesFor(UUID excludeUserId);
}
