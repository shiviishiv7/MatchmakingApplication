package com.shiviishiv7.matchmaking.provider.implementation;

import com.shiviishiv7.matchmaking.provider.model.ChatQuestionBank;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;

@Repository
public interface ChatQuestionBankRepository extends JpaRepository<ChatQuestionBank, Long> {

    List<ChatQuestionBank> findByMatchCategoryAndIsActiveTrueOrderByCreatedAtAsc(String matchCategory);

    boolean existsByMatchCategoryAndQuestionText(String matchCategory, String questionText);
}
