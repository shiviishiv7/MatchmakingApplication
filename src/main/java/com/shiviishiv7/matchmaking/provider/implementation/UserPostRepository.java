package com.shiviishiv7.matchmaking.provider.implementation;

import com.shiviishiv7.matchmaking.common.enums.PostStatus;
import com.shiviishiv7.matchmaking.provider.model.UserPost;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.time.LocalDateTime;
import java.util.List;

@Repository
public interface UserPostRepository extends JpaRepository<UserPost, Long> {

    List<UserPost> findByCognitoSubOrderByCreatedAtDesc(String cognitoSub);

    List<UserPost> findByCognitoSubAndStatusOrderByCreatedAtDesc(String cognitoSub, PostStatus status);

    @Query("""
            SELECT p FROM UserPost p
            WHERE p.status = 'ACTIVE'
              AND p.matchCount < 5
              AND p.expiresAt > :now
            ORDER BY p.createdAt ASC
            """)
    List<UserPost> findActivePostsForMatching(@Param("now") LocalDateTime now);

    @Query("""
            SELECT p FROM UserPost p
            WHERE p.status = 'ACTIVE'
              AND p.expiresAt <= :now
            """)
    List<UserPost> findExpiredActivePosts(@Param("now") LocalDateTime now);
}
