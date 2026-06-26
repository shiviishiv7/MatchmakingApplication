package com.shiviishiv7.matchmaking.provider.implementation;

import com.shiviishiv7.matchmaking.provider.model.ChatSession;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;

@Repository
public interface ChatSessionRepository extends JpaRepository<ChatSession, Long> {

    List<ChatSession> findByCognitoSubOrderByCreatedAtDesc(String cognitoSub);

    List<ChatSession> findByCognitoSubAndStatusOrderByCreatedAtDesc(String cognitoSub, String status);
}
