package com.shiviishiv7.matchmaking.processor.matchingengine;

import com.shiviishiv7.matchmaking.common.exception.MatchmakingException;
import com.shiviishiv7.matchmaking.provider.model.BlockList;
import com.shiviishiv7.matchmaking.provider.implementation.BlockListRepository;
import com.shiviishiv7.matchmaking.provider.implementation.MatchResultRepository;
import com.shiviishiv7.matchmaking.provider.vo.*;
import jakarta.transaction.Transactional;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;

import java.time.LocalDateTime;
import java.util.List;

import static com.shiviishiv7.matchmaking.common.constants.MatchmakingHttpStatus.*;

@Component
@Transactional
@Slf4j
public class MatchingProcessor implements IMatchingProcessor {

    @Autowired private MatchingEngineProcessor matchingEngineProcessor;
    @Autowired private MatchResultRepository matchResultRepository;
    @Autowired private BlockListRepository blockListRepository;

    @Override
    public BaseVO discover(MatchDiscoveryRequestVO request) throws MatchmakingException {
        try {
            log.info("Discovery request for userId: {} category: {}", request.getCognitoSubA(), request.getMatchCategory());
            request.validate();

            List<MatchCandidateVO> results = matchingEngineProcessor.discover(request);
            log.info("Discovery returned {} results for userId: {}", results.size(), request.getCognitoSubA());

            return new BaseVO(SUCCESS, "Discovery results fetched", "Discovery results fetched", results);
        } catch (MatchmakingException ex) {
            throw ex;
        } catch (Exception ex) {
            log.error("ALERT_FOR_ERROR: Error during discovery for userId: {}. Error: {}",
                    request.getCognitoSubA(), ex.getMessage(), ex);
            throw new MatchmakingException("Error during discovery: " + ex.getMessage(), UNKNOWN_EXCEPTION);
        }
    }

    @Override
    public BaseVO block(String userId, String targetUserId, String reason) throws MatchmakingException {
        try {
            log.info("BLOCK action: blockerId={} blockedId={}", userId, targetUserId);
            Integer blocker = Integer.valueOf(userId);
            Integer blocked = Integer.valueOf(targetUserId);

            if (blockListRepository.existsByBlockerIdAndBlockedId(blocker, blocked)) {
                throw new MatchmakingException("User is already blocked", DUPLICATE_RECORD);
            }

            BlockList block = BlockList.builder()
                    .blockerId(blocker)
                    .blockedId(blocked)
                    .reason(reason)
                    .blockedAt(LocalDateTime.now())
                    .build();
            blockListRepository.save(block);
            log.info("User {} blocked by {}", targetUserId, userId);

            return new BaseVO(SUCCESS, "User blocked", "User blocked");
        } catch (MatchmakingException ex) {
            throw ex;
        } catch (Exception ex) {
            log.error("ALERT_FOR_ERROR: Error blocking user. Error: {}", ex.getMessage(), ex);
            throw new MatchmakingException("Error blocking user: " + ex.getMessage(), UNKNOWN_EXCEPTION);
        }
    }

    @Override
    public BaseVO unblock(String userId, String targetUserId) throws MatchmakingException {
        try {
            log.info("UNBLOCK action: blockerId={} blockedId={}", userId, targetUserId);
            blockListRepository.deleteByBlockerIdAndBlockedId(
                    Integer.valueOf(userId), Integer.valueOf(targetUserId));
            log.info("User {} unblocked by {}", targetUserId, userId);
            return new BaseVO(SUCCESS, "User unblocked", "User unblocked");
        } catch (MatchmakingException ex) {
            throw ex;
        } catch (Exception ex) {
            log.error("ALERT_FOR_ERROR: Error unblocking user. Error: {}", ex.getMessage(), ex);
            throw new MatchmakingException("Error unblocking user: " + ex.getMessage(), UNKNOWN_EXCEPTION);
        }
    }
}
