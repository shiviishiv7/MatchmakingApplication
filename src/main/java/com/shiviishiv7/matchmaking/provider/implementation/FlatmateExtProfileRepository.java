package com.shiviishiv7.matchmaking.provider.implementation;

import com.shiviishiv7.matchmaking.provider.model.profile.FlatmateExtProfile;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;
import java.util.Optional;

@Repository
public interface FlatmateExtProfileRepository extends JpaRepository<FlatmateExtProfile, Integer> {

    Optional<FlatmateExtProfile> findByCognitoSub(String cognitoSub);

    boolean existsByCognitoSub(String cognitoSub);

    void deleteByCognitoSub(String cognitoSub);
}
