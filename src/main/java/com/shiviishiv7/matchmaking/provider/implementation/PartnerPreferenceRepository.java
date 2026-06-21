package com.shiviishiv7.matchmaking.provider.implementation;

import com.shiviishiv7.matchmaking.provider.model.PartnerPreference;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;
import java.util.Optional;

@Repository
public interface PartnerPreferenceRepository extends JpaRepository<PartnerPreference, Integer> {

    Optional<PartnerPreference> findByCognitoSub(String cognitoSub);

    boolean existsByCognitoSub(String cognitoSub);

    void deleteByCognitoSub(String cognitoSub);
}
