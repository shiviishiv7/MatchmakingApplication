package com.shiviishiv7.matchmaking.processor.match;

import com.shiviishiv7.matchmaking.common.enums.MatchStatus;
import com.shiviishiv7.matchmaking.common.exception.MatchmakingException;
import com.shiviishiv7.matchmaking.provider.implementation.MatchResultRepository;
import com.shiviishiv7.matchmaking.provider.implementation.UserRepository;

import com.shiviishiv7.matchmaking.provider.model.MatchResult;
import com.shiviishiv7.matchmaking.provider.model.User;
import com.shiviishiv7.matchmaking.provider.vo.BaseVO;
import com.shiviishiv7.matchmaking.provider.vo.MatchResultVO;
import jakarta.transaction.Transactional;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;

import java.util.List;
import java.util.Optional;

import java.util.stream.Collectors;

import static com.shiviishiv7.matchmaking.common.constants.MatchmakingHttpStatus.*;

@Component
@Transactional
@Slf4j
public class MatchProcessor implements IMatchProcessor {

    @Autowired
    private MatchResultRepository matchRepository;



    @Override
    public BaseVO get(String id) throws MatchmakingException {
        try {
            log.info("Fetching match for ID: {}", id);
            Optional<MatchResult> optionalMatch = matchRepository.findById(Integer.valueOf(id));
            if (optionalMatch.isEmpty()) {
                log.error("ALERT_FOR_ERROR: Match not found for ID: {}", id);
                throw new MatchmakingException("Match does not exist", DATA_NOT_FOUND);
            }

            log.info("Match found for ID: {}", id);
            return new BaseVO(SUCCESS, "Match record fetched", "Match record fetched", optionalMatch.get().toVO());
        } catch (MatchmakingException ex) {
            throw ex;
        } catch (Exception ex) {
            log.error("ALERT_FOR_ERROR: Error occurred while fetching match. Error: {}", ex.getMessage(), ex);
            throw new MatchmakingException("Error occurred while fetching match: " + ex.getMessage(), UNKNOWN_EXCEPTION);
        }
    }

    @Override
    public BaseVO getActiveMatchForUser(String userId) throws MatchmakingException {
        try {
            log.info("Fetching active match for user ID: {}", userId);
            Optional<MatchResult> optionalMatch = matchRepository.findMatchByCognitoSubAOrCognitoSubB(userId,userId);
            if (optionalMatch.isEmpty()) {
                log.error("ALERT_FOR_ERROR: No active match found for user ID: {}", userId);
                throw new MatchmakingException("No active match found for this user", DATA_NOT_FOUND);
            }
            MatchResultVO vo = optionalMatch.get().toVO();
            log.info("Active match found for user ID: {}", userId);
            return new BaseVO(SUCCESS, "Active match fetched", "Active match fetched for user", vo);
        } catch (MatchmakingException ex) {
            throw ex;
        } catch (Exception ex) {
            log.error("ALERT_FOR_ERROR: Error occurred while fetching active match. Error: {}", ex.getMessage(), ex);
            throw new MatchmakingException("Error occurred while fetching active match: " + ex.getMessage(), UNKNOWN_EXCEPTION);
        }
    }

    @Override
    public BaseVO getAllByStatus(String status) throws MatchmakingException {
        try {
            log.info("Fetching all matches for status: {}", status);
            if (status == null || status.isBlank()) {
                throw new MatchmakingException("Status cannot be empty", VALIDATION_ERROR);
            }

            MatchStatus matchStatus;
            try {
                matchStatus = MatchStatus.valueOf(status.toUpperCase());
            } catch (IllegalArgumentException ex) {
                log.error("ALERT_FOR_ERROR: Invalid match status: {}", status);
                throw new MatchmakingException("Invalid match status: " + status, VALIDATION_ERROR);
            }

            List<MatchResult> matches = matchRepository.findByStatus(matchStatus);
            if (matches.isEmpty()) {
                log.error("ALERT_FOR_ERROR: No matches found for status: {}", status);
                throw new MatchmakingException("No matches found for the given status", DATA_NOT_FOUND);
            }

            List<MatchResultVO> result = matches.stream()
                    .map(m -> m.toVO())
                    .collect(Collectors.toList());

            log.info("Fetched {} matches for status: {}", result.size(), status);
            return new BaseVO(SUCCESS, "Matches fetched", "Match records fetched by status", result);
        } catch (MatchmakingException ex) {
            throw ex;
        } catch (Exception ex) {
            log.error("ALERT_FOR_ERROR: Error occurred while fetching matches by status. Error: {}", ex.getMessage(), ex);
            throw new MatchmakingException("Error occurred while fetching matches: " + ex.getMessage(), UNKNOWN_EXCEPTION);
        }
    }

    @Override
    public BaseVO end(String id) throws MatchmakingException {
        try {
            log.info("Ending match for ID: {}", id);
            Optional<MatchResult> optionalMatch = matchRepository.findById(Integer.valueOf(id));
            if (optionalMatch.isEmpty()) {
                log.error("ALERT_FOR_ERROR: Match not found for ID: {}", id);
                throw new MatchmakingException("Match does not exist", DATA_NOT_FOUND);
            }

            MatchResult match = optionalMatch.get();
            if (match.getStatus() == MatchStatus.COMPLETED || match.getStatus() == MatchStatus.ENDED) {
                log.error("ALERT_FOR_ERROR: Match with ID {} is already in terminal state: {}", id, match.getStatus());
                throw new MatchmakingException("Match is already in a terminal state: " + match.getStatus(), VALIDATION_ERROR);
            }

            match.setStatus(MatchStatus.ENDED);
            matchRepository.save(match);
            log.info("Match with ID {} has been ended.", id);

            return new BaseVO(SUCCESS, "Match ended", "Match record updated to ENDED");
        } catch (MatchmakingException ex) {
            throw ex;
        } catch (Exception ex) {
            log.error("ALERT_FOR_ERROR: Error occurred while ending match. Error: {}", ex.getMessage(), ex);
            throw new MatchmakingException("Error occurred while ending match: " + ex.getMessage(), UNKNOWN_EXCEPTION);
        }
    }
}
