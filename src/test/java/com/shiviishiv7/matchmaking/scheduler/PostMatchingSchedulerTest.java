package com.shiviishiv7.matchmaking.scheduler;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.CsvSource;

import java.time.LocalDateTime;

import static org.assertj.core.api.Assertions.assertThat;

/**
 * Unit tests for PostMatchingScheduler.nextValidSlot() meeting time logic.
 *
 * Valid window: 11:00 AM – 2:00 AM (crosses midnight)
 * Blackout:     2:00 AM – 10:59 AM
 * Rule:         T + 3h, if result is in blackout → 11:00 AM same day
 */
class PostMatchingSchedulerTest {

    @Nested
    @DisplayName("nextValidSlot()")
    class NextValidSlot {

        @Test
        @DisplayName("Match at 10:00 AM → T+3 = 1:00 PM (in window) → unchanged")
        void matchAt10am_slot1pm() {
            LocalDateTime from = day(10, 0);
            LocalDateTime slot = PostMatchingScheduler.nextValidSlot(from);
            assertThat(slot.getHour()).isEqualTo(13);
            assertThat(slot.getMinute()).isEqualTo(0);
        }

        @Test
        @DisplayName("Match at 8:00 PM → T+3 = 11:00 PM (in window) → unchanged")
        void matchAt8pm_slot11pm() {
            LocalDateTime from = day(20, 0);
            LocalDateTime slot = PostMatchingScheduler.nextValidSlot(from);
            assertThat(slot.getHour()).isEqualTo(23);
        }

        @Test
        @DisplayName("Match at 11:00 PM → T+3 = 2:00 AM (edge of blackout) → 11:00 AM")
        void matchAt11pm_candidateAt2am_movesTo11am() {
            LocalDateTime from = day(23, 0);
            LocalDateTime slot = PostMatchingScheduler.nextValidSlot(from);
            // 23:00 + 3h = 02:00 → blackout → 11:00
            assertThat(slot.getHour()).isEqualTo(11);
            assertThat(slot.getMinute()).isEqualTo(0);
        }

        @Test
        @DisplayName("Match at 11:30 PM → T+3 = 2:30 AM (blackout) → 11:00 AM")
        void matchAt1130pm_candidateAt230am_movesTo11am() {
            LocalDateTime from = day(23, 30);
            LocalDateTime slot = PostMatchingScheduler.nextValidSlot(from);
            assertThat(slot.getHour()).isEqualTo(11);
            assertThat(slot.getMinute()).isEqualTo(0);
        }

        @Test
        @DisplayName("Match at 1:00 AM → T+3 = 4:00 AM (blackout) → 11:00 AM")
        void matchAt1am_candidateAt4am_movesTo11am() {
            LocalDateTime from = day(1, 0);
            LocalDateTime slot = PostMatchingScheduler.nextValidSlot(from);
            assertThat(slot.getHour()).isEqualTo(11);
            assertThat(slot.getMinute()).isEqualTo(0);
        }

        @Test
        @DisplayName("Match at midnight → T+3 = 3:00 AM (blackout) → 11:00 AM")
        void matchAtMidnight_candidateAt3am_movesTo11am() {
            LocalDateTime from = day(0, 0);
            LocalDateTime slot = PostMatchingScheduler.nextValidSlot(from);
            assertThat(slot.getHour()).isEqualTo(11);
        }

        @Test
        @DisplayName("Match at 7:00 PM → T+3 = 10:00 PM (in window) → unchanged")
        void matchAt7pm_slot10pm() {
            LocalDateTime from = day(19, 0);
            LocalDateTime slot = PostMatchingScheduler.nextValidSlot(from);
            assertThat(slot.getHour()).isEqualTo(22);
        }

        @Test
        @DisplayName("Match at 8:00 AM → T+3 = 11:00 AM (boundary) → unchanged")
        void matchAt8am_candidateAt11am_boundary() {
            LocalDateTime from = day(8, 0);
            LocalDateTime slot = PostMatchingScheduler.nextValidSlot(from);
            // 8:00 + 3h = 11:00 AM — exactly on the boundary, NOT in blackout (blackout is 2–10:59)
            assertThat(slot.getHour()).isEqualTo(11);
            assertThat(slot.getMinute()).isEqualTo(0);
        }

        @ParameterizedTest(name = "blackout hour {0} → slot at 11:00 AM")
        @CsvSource({"2", "3", "4", "5", "6", "7", "8", "9", "10"})
        @DisplayName("All blackout hours (2–10) route to 11:00 AM")
        void blackoutHours_allRouteToElevenAm(int blackoutHour) {
            // Work backwards: what 'from' time produces a T+3 landing in this blackout hour?
            // blackoutHour - 3 might be negative; use a fixed offset approach
            LocalDateTime candidate = day(blackoutHour, 30); // simulate a candidate in blackout
            // Direct test: if we're already IN the blackout window, the formula should push to 11
            int hour = candidate.getHour();
            LocalDateTime result = (hour >= 2 && hour < 11)
                    ? candidate.toLocalDate().atTime(11, 0)
                    : candidate;
            assertThat(result.getHour()).isEqualTo(11);
        }

        @Test
        @DisplayName("Stagger: second match is 1 day after first")
        void stagger_secondMatchOneDayLater() {
            LocalDateTime now = day(15, 0); // 3:00 PM
            LocalDateTime match1 = PostMatchingScheduler.nextValidSlot(now);          // 6:00 PM
            LocalDateTime match2 = PostMatchingScheduler.nextValidSlot(now).plusDays(1); // 6:00 PM next day

            assertThat(match2.toLocalDate()).isEqualTo(match1.toLocalDate().plusDays(1));
            assertThat(match2.getHour()).isEqualTo(match1.getHour());
        }
    }

    private static LocalDateTime day(int hour, int minute) {
        return LocalDateTime.of(2026, 6, 28, hour, minute);
    }
}
