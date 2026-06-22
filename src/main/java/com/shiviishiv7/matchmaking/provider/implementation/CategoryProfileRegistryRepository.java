package com.shiviishiv7.matchmaking.provider.implementation;

import com.shiviishiv7.matchmaking.common.enums.MatchCategory;
import com.shiviishiv7.matchmaking.provider.model.CategoryProfileRegistry;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;

@Repository
public interface CategoryProfileRegistryRepository extends JpaRepository<CategoryProfileRegistry, Integer> {

    Optional<CategoryProfileRegistry> findByCognitoSub(String cognitoSub);

    List<CategoryProfileRegistry> findByCognitoSubAndIsActive(String cognitoSub, Boolean isActive);

    Optional<CategoryProfileRegistry> findByCognitoSubAndMatchCategory(String cognitoSub, MatchCategory matchCategory);

    boolean existsByCognitoSubAndMatchCategory(String cognitoSub, MatchCategory matchCategory);

    void deleteByCognitoSubAndMatchCategory(String cognitoSub, MatchCategory matchCategory);
}
