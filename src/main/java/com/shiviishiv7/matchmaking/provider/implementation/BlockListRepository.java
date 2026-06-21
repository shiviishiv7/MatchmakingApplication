package com.shiviishiv7.matchmaking.provider.implementation;

import com.shiviishiv7.matchmaking.provider.model.BlockList;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.util.Set;

@Repository
public interface BlockListRepository extends JpaRepository<BlockList, Integer> {

    boolean existsByBlockerIdAndBlockedId(Integer blockerId, Integer blockedId);

    /** All IDs that userId has blocked OR that have blocked userId */
    @Query("SELECT b.blockedId FROM BlockList b WHERE b.blockerId = :userId " +
           "UNION SELECT b.blockerId FROM BlockList b WHERE b.blockedId = :userId")
    Set<String> findAllBlockedUserIds(@Param("userId") String userId);

    void deleteByBlockerIdAndBlockedId(Integer blockerId, Integer blockedId);
}
