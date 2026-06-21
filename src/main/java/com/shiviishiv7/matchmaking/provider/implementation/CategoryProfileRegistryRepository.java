package com.shiviishiv7.matchmaking.provider.implementation;

import com.shiviishiv7.matchmaking.common.enums.MatchCategory;
import com.shiviishiv7.matchmaking.provider.model.CategoryProfileRegistry;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;

@Repository
public interface CategoryProfileRegistryRepository extends JpaRepository<CategoryProfileRegistry, Integer> {

    List<CategoryProfileRegistry> findByUserId(Integer userId);

    List<CategoryProfileRegistry> findByUserIdAndIsActive(Integer userId, Boolean isActive);

    Optional<CategoryProfileRegistry> findByUserIdAndMatchCategory(Integer userId, MatchCategory matchCategory);

    boolean existsByUserIdAndMatchCategory(Integer userId, MatchCategory matchCategory);

    void deleteByUserIdAndMatchCategory(Integer userId, MatchCategory matchCategory);
}
