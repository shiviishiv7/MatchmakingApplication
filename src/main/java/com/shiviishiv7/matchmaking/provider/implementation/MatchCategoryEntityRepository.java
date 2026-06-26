package com.shiviishiv7.matchmaking.provider.implementation;

import com.shiviishiv7.matchmaking.provider.model.MatchCategoryEntity;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;

@Repository
public interface MatchCategoryEntityRepository extends JpaRepository<MatchCategoryEntity, Integer> {

    List<MatchCategoryEntity> findAllByIsActiveTrueOrderByDisplayOrderAsc();

    List<MatchCategoryEntity> findAllByGroupIdAndIsActiveTrueOrderByDisplayOrderAsc(Integer groupId);

    Optional<MatchCategoryEntity> findByEnumKeyIgnoreCase(String enumKey);
}
