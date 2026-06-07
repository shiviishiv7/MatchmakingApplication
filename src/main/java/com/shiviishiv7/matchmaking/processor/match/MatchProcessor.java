package com.shiviishiv7.matchmaking.processor.match;

import com.shiviishiv7.matchmaking.common.enums.MatchStatus;
import com.shiviishiv7.matchmaking.common.exception.MatchmakingException;
import com.shiviishiv7.matchmaking.provider.implementation.MatchRepository;
import com.shiviishiv7.matchmaking.provider.implementation.UserRepository;
import com.shiviishiv7.matchmaking.provider.model.Match;
import com.shiviishiv7.matchmaking.provider.model.User;
import com.shiviishiv7.matchmaking.provider.vo.BaseVO;
import com.shiviishiv7.matchmaking.provider.vo.MatchVO;
import jakarta.transaction.Transactional;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;

import java.util.List;
import java.util.Optional;
import java.util.UUID;
import java.util.stream.Collectors;

import static com.shiviishiv7.matchmaking.common.constants.MatchmakingHttpStatus.*;

@Component
@Transactional
@Slf4j
public class MatchProcessor implements IMatchProcessor {

    @Autowired
    private MatchRepository matchRepository;

    @Autowired
    private UserRepository userRepository;

    @Override
    public BaseVO add(MatchVO matchVO) throws MatchmakingException {
        try {
            log.info("Validating inputs for match creation.");
            matchVO.validate();
            log.info("MatchVO validation completed successfully.");

            log.trace("Fetching user A for ID: {}", matchVO.getUserAId());
            Optional<User> optionalUserA = userRepository.findById(matchVO.getUserAId());
            if (optionalUserA.isEmpty()) {
                log.error("ALERT_FOR_ERROR: User A not found for ID: {}", matchVO.getUserAId());
                throw new MatchmakingException("User A does not exist", DATA_NOT_FOUND);
            }

            log.trace("Fetching user B for ID: {}", matchVO.getUserBId());
            Optional<User> optionalUserB = userRepository.findById(matchVO.getUserBId());
            if (optionalUserB.isEmpty()) {
                log.error("ALERT_FOR_ERROR: User B not found for ID: {}", matchVO.getUserBId());
                throw new MatchmakingException("User B does not exist", DATA_NOT_FOUND);
            }

            log.trace("Checking if match already exists between users {} and {}", matchVO.getUserAId(), matchVO.getUserBId());
            if (matchRepository.existsMatchBetween(matchVO.getUserAId(), matchVO.getUserBId())) {
                log.error("ALERT_FOR_ERROR: Match already exists between users {} and {}", matchVO.getUserAId(), matchVO.getUserBId());
                throw new MatchmakingException("A match already exists between these users", DUPLICATE_RECORD);
            }

            log.trace("Saving match record.");
            Match match = matchVO.fromVO();
            match.setUserA(optionalUserA.get());
            match.setUserB(optionalUserB.get());
            match = matchRepository.save(match);
            log.info("Match record saved successfully with ID: {}", match.getId());

            return new BaseVO(SUCCESS, "Match record saved", "Match record saved", new MatchVO().toVO(match));
        } catch (MatchmakingException ex) {
            throw ex;
        } catch (Exception ex) {
            log.error("ALERT_FOR_ERROR: Error occurred while adding match. Error: {}", ex.getMessage(), ex);
            throw new MatchmakingException("Error occurred while adding match: " + ex.getMessage(), UNKNOWN_EXCEPTION);
        }
    }

    @Override
    public BaseVO get(UUID id) throws MatchmakingException {
        try {
            log.info("Fetching match for ID: {}", id);
            Optional<Match> optionalMatch = matchRepository.findById(id);
            if (optionalMatch.isEmpty()) {
                log.error("ALERT_FOR_ERROR: Match not found for ID: {}", id);
                throw new MatchmakingException("Match does not exist", DATA_NOT_FOUND);
            }

            log.info("Match found for ID: {}", id);
            return new BaseVO(SUCCESS, "Match record fetched", "Match record fetched", new MatchVO().toVO(optionalMatch.get()));
        } catch (MatchmakingException ex) {
            throw ex;
        } catch (Exception ex) {
            log.error("ALERT_FOR_ERROR: Error occurred while fetching match. Error: {}", ex.getMessage(), ex);
            throw new MatchmakingException("Error occurred while fetching match: " + ex.getMessage(), UNKNOWN_EXCEPTION);
        }
    }

    @Override
    public BaseVO getActiveMatchForUser(UUID userId) throws MatchmakingException {
        try {
            log.info("Fetching active match for user ID: {}", userId);
            Optional<Match> optionalMatch = matchRepository.findActiveMatchForUser(userId);
            if (optionalMatch.isEmpty()) {
                log.error("ALERT_FOR_ERROR: No active match found for user ID: {}", userId);
                throw new MatchmakingException("No active match found for this user", DATA_NOT_FOUND);
            }

            log.info("Active match found for user ID: {}", userId);
            return new BaseVO(SUCCESS, "Active match fetched", "Active match fetched for user", new MatchVO().toVO(optionalMatch.get()));
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

            List<Match> matches = matchRepository.findByStatus(matchStatus);
            if (matches.isEmpty()) {
                log.error("ALERT_FOR_ERROR: No matches found for status: {}", status);
                throw new MatchmakingException("No matches found for the given status", DATA_NOT_FOUND);
            }

            List<MatchVO> result = matches.stream()
                    .map(m -> new MatchVO().toVO(m))
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
    public BaseVO end(UUID id) throws MatchmakingException {
        try {
            log.info("Ending match for ID: {}", id);
            Optional<Match> optionalMatch = matchRepository.findById(id);
            if (optionalMatch.isEmpty()) {
                log.error("ALERT_FOR_ERROR: Match not found for ID: {}", id);
                throw new MatchmakingException("Match does not exist", DATA_NOT_FOUND);
            }

            Match match = optionalMatch.get();
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
