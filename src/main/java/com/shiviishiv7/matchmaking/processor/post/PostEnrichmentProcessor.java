package com.shiviishiv7.matchmaking.processor.post;

import com.shiviishiv7.matchmaking.processor.matchingengine.MatchingEngineProcessor;
import com.shiviishiv7.matchmaking.provider.implementation.UserPostRepository;
import com.shiviishiv7.matchmaking.provider.vo.MatchCandidateVO;
import com.shiviishiv7.matchmaking.provider.vo.MatchDiscoveryRequestVO;
import com.shiviishiv7.matchmaking.provider.vo.MatchFilterVO;
import com.shiviishiv7.matchmaking.provider.vo.ws.MatchNotificationVO;
import com.shiviishiv7.matchmaking.common.enums.MatchCategory;
import com.shiviishiv7.matchmaking.service.match.MatchConnectService;
import com.shiviishiv7.matchmaking.service.presence.UserPresenceService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.messaging.simp.SimpMessagingTemplate;
import org.springframework.stereotype.Service;

import java.util.List;

@Service
@Slf4j
@RequiredArgsConstructor
public class PostEnrichmentProcessor {

    private static final String USER_QUEUE_MATCHES = "/queue/matches";

    private final UserPostRepository userPostRepository;
    private final MatchingEngineProcessor matchingEngineProcessor;
    private final MatchConnectService matchConnectService;
    private final SimpMessagingTemplate messagingTemplate;
    private final IPostProfileUpsertService postProfileUpsertService;
    private final UserPresenceService userPresenceService;

    public void enrich(PostEnrichmentTask task) {
        MatchFilterVO filterVO = task.filterVO();
        Long postId = task.postId();
        String cognitoSub = filterVO.getCognitoSub();
        try {
            postProfileUpsertService.upsert(filterVO);
            log.info("Profile upserted for cognitoSub={} category={}", cognitoSub, filterVO.getChildCategory());

            userPostRepository.findById(postId).ifPresent(p -> {
                p.setProfileUpdated(true);
                userPostRepository.save(p);
            });

            MatchDiscoveryRequestVO discoveryRequest = new MatchDiscoveryRequestVO();
            discoveryRequest.setCognitoSubA(cognitoSub);
            discoveryRequest.setMatchCategory(MatchCategory.valueOf(filterVO.getChildCategory()));
            discoveryRequest.setPage(0);
            discoveryRequest.setPageSize(20);

            List<MatchCandidateVO> candidates = matchingEngineProcessor.discover(discoveryRequest);
            log.info("Discovery saved {} PENDING matches for cognitoSub={}", candidates.size(), cognitoSub);

            if (candidates.isEmpty()) {
                // Mark this user as actively waiting so they can be reverse-notified
                // when a future submission finds them as a candidate
                userPresenceService.markAsLooking(cognitoSub);
                messagingTemplate.convertAndSendToUser(cognitoSub, USER_QUEUE_MATCHES,
                        MatchNotificationVO.builder()
                                .event("POST_NO_MATCH_FOUND")
                                .message("No match found yet. We'll notify you when someone matches your profile.")
                                .build());
                return;
            }

            // Notify any waiting users who appear in our candidate list —
            // they were in POST_NO_MATCH_FOUND state and are still on the waiting page
            matchConnectService.notifyWaitingPostMatchCandidates(candidates, cognitoSub);

            boolean connected = matchConnectService.connectNextOnlineMatch(cognitoSub);
            if (connected) {
                userPresenceService.markAsNotLooking(cognitoSub);
                messagingTemplate.convertAndSendToUser(cognitoSub, USER_QUEUE_MATCHES,
                        MatchNotificationVO.builder()
                                .event("POST_MATCH_CONNECTING")
                                .message("Match found and connecting now! Check your call screen.")
                                .build());
            } else {
                MatchCandidateVO top = candidates.get(0);
                matchConnectService.sendNoOnlineMatchEmails(cognitoSub, top.getCognitoSubB());

                // Mark current user as waiting too — if their matched candidate comes online later
                userPresenceService.markAsLooking(cognitoSub);
                messagingTemplate.convertAndSendToUser(cognitoSub, USER_QUEUE_MATCHES,
                        MatchNotificationVO.builder()
                                .event("POST_NO_ACTIVE_MATCH")
                                .message("Match saved! No one is online right now. We'll notify you when your match comes online.")
                                .build());
                log.info("No online candidate found for cognitoSub={} — emails sent, waiting", cognitoSub);
            }

        } catch (Exception e) {
            log.error("ALERT_FOR_ERROR: Post-submit enrichment failed for postId={}: {}", postId, e.getMessage(), e);
            messagingTemplate.convertAndSendToUser(filterVO.getCognitoSub(), USER_QUEUE_MATCHES,
                    MatchNotificationVO.builder()
                            .event("POST_MATCH_ERROR")
                            .message("We saved your post but couldn't run matching right now. Please try again shortly.")
                            .build());
        }
    }
}
