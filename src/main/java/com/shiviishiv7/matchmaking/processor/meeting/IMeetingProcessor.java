package com.shiviishiv7.matchmaking.processor.meeting;

import com.shiviishiv7.matchmaking.common.exception.MatchmakingException;
import com.shiviishiv7.matchmaking.provider.vo.BaseVO;
import com.shiviishiv7.matchmaking.provider.vo.MeetingVO;

import java.util.UUID;

public interface IMeetingProcessor {

    BaseVO add(MeetingVO meetingVO) throws MatchmakingException;

    BaseVO get(UUID id) throws MatchmakingException;

    BaseVO getAllForMatch(UUID matchId) throws MatchmakingException;

    BaseVO markCompleted(UUID id) throws MatchmakingException;

    BaseVO getUpcomingMeetings(String sub) throws MatchmakingException;
}
