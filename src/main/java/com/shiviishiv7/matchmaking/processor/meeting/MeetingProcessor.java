package com.shiviishiv7.matchmaking.processor.meeting;

import com.shiviishiv7.matchmaking.common.enums.MatchStatus;
import com.shiviishiv7.matchmaking.common.enums.MeetingStatus;
import com.shiviishiv7.matchmaking.common.exception.MatchmakingException;
import com.shiviishiv7.matchmaking.provider.implementation.MatchResultRepository;
import com.shiviishiv7.matchmaking.provider.implementation.MeetingRepository;

import com.shiviishiv7.matchmaking.provider.model.MatchResult;
import com.shiviishiv7.matchmaking.provider.model.Meeting;
import com.shiviishiv7.matchmaking.provider.vo.BaseVO;
import com.shiviishiv7.matchmaking.provider.vo.MeetingVO;
import jakarta.transaction.Transactional;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;

import java.time.LocalDateTime;
import java.util.List;
import java.util.Optional;

import java.util.stream.Collectors;

import static com.shiviishiv7.matchmaking.common.constants.MatchmakingHttpStatus.*;

// UserRepository is needed to resolve the peer for upcoming meetings

@Component
@Transactional
@Slf4j
public class MeetingProcessor implements IMeetingProcessor {

    @Autowired
    private MeetingRepository meetingRepository;

    @Autowired
    private MatchResultRepository matchRepository;

    @Override
    public BaseVO add(MeetingVO meetingVO) throws MatchmakingException {
        try {
            log.info("Validating inputs for meeting creation.");
            meetingVO.validate();
            log.info("MeetingVO validation completed successfully.");

            log.trace("Fetching match for ID: {}", meetingVO.getMatchId());
            Optional<MatchResult> optionalMatch = matchRepository.findById(Integer.valueOf(meetingVO.getMatchId()));
            if (optionalMatch.isEmpty()) {
                log.error("ALERT_FOR_ERROR: Match not found for ID: {}", meetingVO.getMatchId());
                throw new MatchmakingException("Match does not exist", DATA_NOT_FOUND);
            }

            log.trace("Saving meeting record for match ID: {}", meetingVO.getMatchId());
            Meeting meeting = meetingVO.fromVO();
//            meeting.setMatch(optionalMatch.get());
            meeting = meetingRepository.save(meeting);
            log.info("Meeting record saved successfully with ID: {}", meeting.getId());

            // Transition match status to MEETING_SCHEDULED so the scheduler knows to watch it
            MatchResult match = optionalMatch.get();
            if (match.getStatus() == MatchStatus.PENDING || match.getStatus() == MatchStatus.ANOTHER_ROUND) {
                match.setStatus(MatchStatus.MEETING_SCHEDULED);
                matchRepository.save(match);
                log.info("Match ID: {} transitioned to MEETING_SCHEDULED.", match.getId());
            }

            return new BaseVO(SUCCESS, "Meeting record saved", "Meeting record saved", new MeetingVO().toVO(meeting));
        } catch (MatchmakingException ex) {
            throw ex;
        } catch (Exception ex) {
            log.error("ALERT_FOR_ERROR: Error occurred while adding meeting. Error: {}", ex.getMessage(), ex);
            throw new MatchmakingException("Error occurred while adding meeting: " + ex.getMessage(), UNKNOWN_EXCEPTION);
        }
    }

    @Override
    public BaseVO get(String id) throws MatchmakingException {
        try {
            log.info("Fetching meeting for ID: {}", id);
            Optional<Meeting> optionalMeeting = meetingRepository.findById(Integer.valueOf(id));
            if (optionalMeeting.isEmpty()) {
                log.error("ALERT_FOR_ERROR: Meeting not found for ID: {}", id);
                throw new MatchmakingException("Meeting does not exist", DATA_NOT_FOUND);
            }

            log.info("Meeting found for ID: {}", id);
            return new BaseVO(SUCCESS, "Meeting record fetched", "Meeting record fetched", new MeetingVO().toVO(optionalMeeting.get()));
        } catch (MatchmakingException ex) {
            throw ex;
        } catch (Exception ex) {
            log.error("ALERT_FOR_ERROR: Error occurred while fetching meeting. Error: {}", ex.getMessage(), ex);
            throw new MatchmakingException("Error occurred while fetching meeting: " + ex.getMessage(), UNKNOWN_EXCEPTION);
        }
    }

    @Override
    public BaseVO getAllForMatch(String matchId) throws MatchmakingException {
        try {
            log.info("Fetching all meetings for match ID: {}", matchId);
            List<Meeting> meetings = meetingRepository.findByMatchId(matchId);
            if (meetings.isEmpty()) {
                log.error("ALERT_FOR_ERROR: No meetings found for match ID: {}", matchId);
                throw new MatchmakingException("No meetings found for the given match", DATA_NOT_FOUND);
            }

            List<MeetingVO> result = meetings.stream()
                    .map(m -> new MeetingVO().toVO(m))
                    .collect(Collectors.toList());

            log.info("Fetched {} meetings for match ID: {}", result.size(), matchId);
            return new BaseVO(SUCCESS, "Meetings fetched", "Meeting records fetched for match", result);
        } catch (MatchmakingException ex) {
            throw ex;
        } catch (Exception ex) {
            log.error("ALERT_FOR_ERROR: Error occurred while fetching meetings for match. Error: {}", ex.getMessage(), ex);
            throw new MatchmakingException("Error occurred while fetching meetings: " + ex.getMessage(), UNKNOWN_EXCEPTION);
        }
    }

    @Override
    public BaseVO markCompleted(String id) throws MatchmakingException {
        try {
            log.info("Marking meeting as completed for ID: {}", id);
            Optional<Meeting> optionalMeeting = meetingRepository.findById(Integer.valueOf(id));
            if (optionalMeeting.isEmpty()) {
                log.error("ALERT_FOR_ERROR: Meeting not found for ID: {}", id);
                throw new MatchmakingException("Meeting does not exist", DATA_NOT_FOUND);
            }

            Meeting meeting = optionalMeeting.get();
            if (meeting.getStatus() == MeetingStatus.COMPLETED || meeting.getStatus() == MeetingStatus.CANCELLED) {
                log.error("ALERT_FOR_ERROR: Meeting with ID {} is already in terminal state: {}", id, meeting.getStatus());
                throw new MatchmakingException("Meeting is already in a terminal state: " + meeting.getStatus(), VALIDATION_ERROR);
            }

            meeting.setStatus(MeetingStatus.COMPLETED);
            meetingRepository.save(meeting);
            log.info("Meeting with ID {} has been marked as completed.", id);

            return new BaseVO(SUCCESS, "Meeting marked as completed", "Meeting status updated to COMPLETED");
        } catch (MatchmakingException ex) {
            throw ex;
        } catch (Exception ex) {
            log.error("ALERT_FOR_ERROR: Error occurred while marking meeting completed. Error: {}", ex.getMessage(), ex);
            throw new MatchmakingException("Error occurred while updating meeting: " + ex.getMessage(), UNKNOWN_EXCEPTION);
        }
    }

    @Override
    public BaseVO getUpcomingMeetings(String sub) throws MatchmakingException {
        try {
            log.info("Fetching upcoming meetings for user sub: {}", sub);
            List<Meeting> meetings = meetingRepository.findUpcomingForUser(sub, LocalDateTime.now());

            List<MeetingVO> result = meetings.stream().map(m -> {
                MeetingVO vo = new MeetingVO().toVO(m);
                // Resolve the peer — the other participant in the match
//                var match = m.getMatch();
//                if (match != null) {
//                    boolean iAmA = match.getUserA().getCognitoSub().equals(sub);
//                    var peer = iAmA ? match.getUserB() : match.getUserA();
//                    vo.setPeerFirstName(peer.getFirstName());
//                    vo.setPeerLastName(peer.getLastName());
//                    vo.setPeerCognitoSub(peer.getCognitoSub());
//                }
                return vo;
            }).collect(Collectors.toList());

            log.info("Found {} upcoming meetings for user {}", result.size(), sub);
            return new BaseVO(SUCCESS, "Upcoming meetings fetched", "Upcoming meetings fetched", result);
        } catch (Exception ex) {
            log.error("ALERT_FOR_ERROR: Error fetching upcoming meetings. Error: {}", ex.getMessage(), ex);
            throw new MatchmakingException("Error fetching upcoming meetings: " + ex.getMessage(), UNKNOWN_EXCEPTION);
        }
    }
}
