package com.shiviishiv7.matchmaking.provider.implementation;

import com.shiviishiv7.matchmaking.provider.model.MeetingFeedback;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;


@Repository
public interface MeetingFeedbackRepository extends JpaRepository<MeetingFeedback, Integer> {

    List<MeetingFeedback> findByMeetingId(String meetingId);

    Optional<MeetingFeedback> findByMeetingIdAndUserId(String meetingId, String userId);

    boolean existsByMeetingIdAndUserId(String meetingId, String userId);

    long countByMeetingId(String meetingId);
}
