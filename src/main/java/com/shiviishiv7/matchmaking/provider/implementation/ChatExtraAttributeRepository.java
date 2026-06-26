package com.shiviishiv7.matchmaking.provider.implementation;

import com.shiviishiv7.matchmaking.provider.model.ChatExtraAttribute;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;

@Repository
public interface ChatExtraAttributeRepository extends JpaRepository<ChatExtraAttribute, Long> {

    List<ChatExtraAttribute> findBySessionId(Long sessionId);

    List<ChatExtraAttribute> findByCognitoSubAndMatchCategory(String cognitoSub, String matchCategory);
}
