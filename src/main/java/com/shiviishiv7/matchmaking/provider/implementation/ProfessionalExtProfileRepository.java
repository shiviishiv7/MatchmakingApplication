package com.shiviishiv7.matchmaking.provider.implementation;

import com.shiviishiv7.matchmaking.provider.model.profile.ProfessionalExtProfile;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;
import java.util.Optional;

@Repository
public interface ProfessionalExtProfileRepository extends JpaRepository<ProfessionalExtProfile, Integer> {
    Optional<ProfessionalExtProfile> findByCognitoSub(String cognitoSub);
    boolean existsByCognitoSub(String cognitoSub);
    void deleteByCognitoSub(String cognitoSub);
}
