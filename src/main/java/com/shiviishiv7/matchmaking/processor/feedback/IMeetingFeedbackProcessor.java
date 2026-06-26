package com.shiviishiv7.matchmaking.processor.feedback;

import com.shiviishiv7.matchmaking.common.exception.MatchmakingException;
import com.shiviishiv7.matchmaking.provider.vo.BaseVO;
import com.shiviishiv7.matchmaking.provider.vo.MeetingFeedbackVO;

public interface IMeetingFeedbackProcessor {

    /** Submit YES/NO feedback for a completed meeting. cognitoSub must be set on the VO. */
    BaseVO submit(MeetingFeedbackVO feedbackVO) throws MatchmakingException;

    BaseVO get(String id) throws MatchmakingException;

    BaseVO getAllForMeeting(String meetingId) throws MatchmakingException;
}
