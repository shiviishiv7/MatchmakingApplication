package com.shiviishiv7.matchmaking.provider.implementation;

import com.shiviishiv7.matchmaking.provider.model.BaseUserProfile;
import com.shiviishiv7.matchmaking.provider.model.User;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;
import java.util.Optional;

@Repository
public interface BaseUserProfileRepository extends JpaRepository<BaseUserProfile, Integer> {

    Optional<BaseUserProfile> findByUserId(Integer userId);

    boolean existsByCognitoSub(String cognitoSub);

    void deleteByUserId(Integer userId);

    Optional<BaseUserProfile> findByEmail(String email);
}
