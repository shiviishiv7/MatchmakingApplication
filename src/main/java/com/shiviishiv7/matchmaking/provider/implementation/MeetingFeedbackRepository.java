package com.shiviishiv7.matchmaking.provider.implementation;

import com.shiviishiv7.matchmaking.provider.model.MeetingFeedback;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;
import java.util.UUID;

@Repository
public interface MeetingFeedbackRepository extends JpaRepository<MeetingFeedback, UUID> {

    List<MeetingFeedback> findByMeetingId(UUID meetingId);

    Optional<MeetingFeedback> findByMeetingIdAndUserId(UUID meetingId, UUID userId);

    boolean existsByMeetingIdAndUserId(UUID meetingId, UUID userId);

    long countByMeetingId(UUID meetingId);
}
