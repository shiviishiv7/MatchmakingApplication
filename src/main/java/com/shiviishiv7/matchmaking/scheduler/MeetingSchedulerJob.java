package com.shiviishiv7.matchmaking.scheduler;

import com.shiviishiv7.matchmaking.common.enums.MatchStatus;
import com.shiviishiv7.matchmaking.common.enums.MeetingStatus;
import com.shiviishiv7.matchmaking.provider.implementation.MatchRepository;
import com.shiviishiv7.matchmaking.provider.implementation.MeetingRepository;
import com.shiviishiv7.matchmaking.provider.model.Match;
import com.shiviishiv7.matchmaking.provider.model.Meeting;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;
import java.util.List;

@Component
@Slf4j
public class MeetingSchedulerJob {

    @Autowired
    private MeetingRepository meetingRepository;

    @Autowired
    private MatchRepository matchRepository;

    /**
     * Runs every 5 minutes.
     * Finds meetings whose scheduled time has passed but are still SCHEDULED,
     * marks them COMPLETED, and transitions the match to AWAITING_FEEDBACK.
     */
    @Scheduled(fixedDelay = 5 * 60 * 1000)
    @Transactional
    public void markCompletedMeetings() {
        log.info("MeetingSchedulerJob: scanning for past meetings to mark as COMPLETED.");

        LocalDateTime cutoff = LocalDateTime.now();
        List<Meeting> pastMeetings = meetingRepository.findCompletedButNotMarked(cutoff);

        if (pastMeetings.isEmpty()) {
            log.info("MeetingSchedulerJob: no past meetings found to process.");
            return;
        }

        log.info("MeetingSchedulerJob: found {} meeting(s) to mark as COMPLETED.", pastMeetings.size());

        for (Meeting meeting : pastMeetings) {
            try {
                meeting.setStatus(MeetingStatus.COMPLETED);
                meetingRepository.save(meeting);
                log.info("MeetingSchedulerJob: marked meeting ID: {} as COMPLETED.", meeting.getId());

                // Transition the match to AWAITING_FEEDBACK so both users are prompted
                Match match = meeting.getMatch();
                if (match != null && match.getStatus() == MatchStatus.MEETING_SCHEDULED) {
                    match.setStatus(MatchStatus.AWAITING_FEEDBACK);
                    matchRepository.save(match);
                    log.info("MeetingSchedulerJob: match ID: {} transitioned to AWAITING_FEEDBACK.", match.getId());
                }
            } catch (Exception ex) {
                log.error("ALERT_FOR_ERROR: MeetingSchedulerJob failed to process meeting ID: {}. Error: {}",
                        meeting.getId(), ex.getMessage(), ex);
            }
        }
    }
}
