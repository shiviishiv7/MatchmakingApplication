package com.shiviishiv7.matchmaking.scheduler;

import com.shiviishiv7.matchmaking.common.enums.MatchStatus;
import com.shiviishiv7.matchmaking.common.enums.MeetingStatus;
import com.shiviishiv7.matchmaking.provider.implementation.MatchRepository;
import com.shiviishiv7.matchmaking.provider.implementation.MeetingRepository;
import com.shiviishiv7.matchmaking.provider.model.Match;
import com.shiviishiv7.matchmaking.provider.model.Meeting;
import com.shiviishiv7.matchmaking.provider.vo.ws.MeetingNotificationVO;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.messaging.simp.SimpMessagingTemplate;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;
import java.util.List;
import java.util.Optional;


@Component
@Slf4j
public class MeetingSchedulerJob {

    @Autowired
    private MeetingRepository meetingRepository;

    @Autowired
    private MatchRepository matchRepository;

    @Autowired
    private SimpMessagingTemplate messagingTemplate;

    /**
     * Runs every minute.
     * Finds SCHEDULED meetings whose scheduledAt has arrived,
     * transitions them to WAITING_ROOM, and notifies both users via WebSocket
     * so they can open the in-app call screen.
     */
    @Scheduled(fixedDelay = 60 * 1000)
    @Transactional
    public void openWaitingRooms() {
        LocalDateTime now = LocalDateTime.now();
        List<Meeting> readyMeetings = meetingRepository.findReadyToOpenWaitingRoom(now);

        if (readyMeetings.isEmpty()) return;

        log.info("MeetingSchedulerJob: opening waiting room for {} meeting(s).", readyMeetings.size());

        for (Meeting meeting : readyMeetings) {
            try {
                meeting.setStatus(MeetingStatus.WAITING_ROOM);
                meetingRepository.save(meeting);
                Optional<Match> optionalMatch = matchRepository.findById(meeting.getId());
                Match match = optionalMatch.get();

                if (match == null) continue;

                String subA = match.getCognitoSubA();
                String subB = match.getCognitoSubB();

                MeetingNotificationVO notification = MeetingNotificationVO.waitingRoom(
                        meeting.getId().toString(),
                        match.getId().toString()
                );

                messagingTemplate.convertAndSendToUser(subA, "/queue/meeting", notification);
                messagingTemplate.convertAndSendToUser(subB, "/queue/meeting", notification);

                log.info("MeetingSchedulerJob: waiting room opened for meeting ID: {}, notified {} and {}.",
                        meeting.getId(), subA, subB);
            } catch (Exception ex) {
                log.error("ALERT_FOR_ERROR: MeetingSchedulerJob failed to open waiting room for meeting ID: {}. Error: {}",
                        meeting.getId(), ex.getMessage(), ex);
            }
        }
    }

    /**
     * Runs every 5 minutes.
     * Finds WAITING_ROOM or IN_PROGRESS meetings whose duration has elapsed,
     * marks them COMPLETED, and transitions the match to AWAITING_FEEDBACK.
     */
    @Scheduled(fixedDelay = 5 * 60 * 1000)
    @Transactional
    public void markExpiredMeetingsCompleted() {
        LocalDateTime now = LocalDateTime.now();
        List<Meeting> expiredMeetings = meetingRepository.findExpiredActiveMeetings(now);

        if (expiredMeetings.isEmpty()) {
            log.info("MeetingSchedulerJob: no expired meetings found.");
            return;
        }

        log.info("MeetingSchedulerJob: found {} expired meeting(s) to mark COMPLETED.", expiredMeetings.size());

        for (Meeting meeting : expiredMeetings) {
            try {
                meeting.setStatus(MeetingStatus.COMPLETED);
                meetingRepository.save(meeting);
                log.info("MeetingSchedulerJob: marked meeting ID: {} as COMPLETED.", meeting.getId());

                Optional<Match> optionalMatch = matchRepository.findById(Integer.valueOf(meeting.getMatchId()));
                Match match = optionalMatch.get();
                if (match != null && match.getStatus() == MatchStatus.MEETING_SCHEDULED) {
                    match.setStatus(MatchStatus.AWAITING_FEEDBACK);
                    matchRepository.save(match);
                    log.info("MeetingSchedulerJob: match ID: {} transitioned to AWAITING_FEEDBACK.", match.getId());
                }
            } catch (Exception ex) {
                log.error("ALERT_FOR_ERROR: MeetingSchedulerJob failed to complete meeting ID: {}. Error: {}",
                        meeting.getId(), ex.getMessage(), ex);
            }
        }
    }
}
