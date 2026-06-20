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
public interface UserRepository extends JpaRepository<User, Integer> {

    Optional<User> findByCognitoSub(String cognitoSub);

    Optional<User> findByEmail(String email);

    boolean existsByEmail(String email);

    boolean existsByCognitoSub(String cognitoSub);

    // Fetch all users in the pool — the matching scheduler uses this
    @Query("SELECT u FROM User u WHERE u.status = :status AND u.isActive = true")
    List<User> findAllByStatus(UserStatus status);


//    List<User> findCandidatesFor(String excludeUserId);

    Optional<User> findById(Integer candidateId);
}
