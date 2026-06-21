package com.shiviishiv7.matchmaking.provider.implementation;

import com.shiviishiv7.matchmaking.provider.model.profile.MatrimonialExtProfile;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;
import java.util.Optional;

@Repository
public interface MatrimonialExtProfileRepository extends JpaRepository<MatrimonialExtProfile, Integer> {

    Optional<MatrimonialExtProfile> findByCognitoSubB(String cognitoSubB);

    boolean existsByCognitoSubB(String cognitoSubB);

    void deleteByCognitoSubB(String cognitoSubB);
}
