package com.shiviishiv7.matchmaking.provider.implementation;

import com.shiviishiv7.matchmaking.provider.model.MatchCategoryGroup;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;

@Repository
public interface MatchCategoryGroupRepository extends JpaRepository<MatchCategoryGroup, Integer> {

    List<MatchCategoryGroup> findAllByIsActiveTrueOrderByDisplayOrderAsc();

    Optional<MatchCategoryGroup> findByNameIgnoreCase(String name);
}
