package com.shiviishiv7.matchmaking.provider.implementation;

import com.shiviishiv7.matchmaking.provider.model.UserPreference;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.Optional;


@Repository
public interface UserPreferenceRepository extends JpaRepository<UserPreference, Integer> {

    Optional<UserPreference> findByUserId(String userId);

    boolean existsByUserId(String userId);
    boolean existsByCognitoSub(String userId);
}
