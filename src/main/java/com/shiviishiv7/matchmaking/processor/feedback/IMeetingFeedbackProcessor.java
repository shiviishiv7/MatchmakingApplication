//package com.shiviishiv7.matchmaking.processor.feedback;
//
//import com.shiviishiv7.matchmaking.common.exception.MatchmakingException;
//import com.shiviishiv7.matchmaking.provider.vo.BaseVO;
//import com.shiviishiv7.matchmaking.provider.vo.MeetingFeedbackVO;
//
//
//public interface IMeetingFeedbackProcessor {
//
//    BaseVO add(MeetingFeedbackVO feedbackVO) throws MatchmakingException;
//
//    BaseVO get(String id) throws MatchmakingException;
//
//    BaseVO getAllForMeeting(String meetingId) throws MatchmakingException;
//
//    default BaseVO submit(MeetingFeedbackVO vo) throws MatchmakingException {
//        return add(vo);
//    }
//
//    default BaseVO getByMeeting(String meetingId) throws MatchmakingException {
//        return getAllForMeeting(meetingId);
//    }
//}
