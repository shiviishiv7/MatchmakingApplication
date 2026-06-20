package com.shiviishiv7.matchmaking.processor.meeting;

import com.shiviishiv7.matchmaking.common.exception.MatchmakingException;
import com.shiviishiv7.matchmaking.provider.vo.BaseVO;
import com.shiviishiv7.matchmaking.provider.vo.MeetingVO;



public interface IMeetingProcessor {

    BaseVO add(MeetingVO meetingVO) throws MatchmakingException;

    BaseVO get(String id) throws MatchmakingException;

    BaseVO getAllForMatch(String matchId) throws MatchmakingException;

    BaseVO markCompleted(String id) throws MatchmakingException;

    BaseVO getUpcomingMeetings(String sub) throws MatchmakingException;
}
