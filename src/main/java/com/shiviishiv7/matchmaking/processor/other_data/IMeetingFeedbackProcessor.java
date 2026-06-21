package com.shiviishiv7.matchmaking.processor.other_data;

import com.shiviishiv7.matchmaking.common.exception.MatchmakingException;
import com.shiviishiv7.matchmaking.provider.vo.BaseVO;
import com.shiviishiv7.matchmaking.provider.vo.MeetingFeedbackVO;

public interface IMeetingFeedbackProcessor {

    BaseVO submit(MeetingFeedbackVO vo) throws MatchmakingException;

    BaseVO getByMeeting(String meetingId) throws MatchmakingException;
}
