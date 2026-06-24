package com.shiviishiv7.matchmaking.provider.vo.ws;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.LocalDate;
import java.util.List;

/**
 * A lightweight view of a user shown in the available-users pool.
 * Carries enough data for client-side display and server-side filter matching.
 */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class PoolUserVO {
    private String cognitoSub;
    private String firstName;
    private String lastName;
    private String gender;          // Gender enum name, e.g. "MALE"
    private String currentCity;
    private LocalDate dateOfBirth;
    private List<String> matchCategories; // active MatchCategory names, e.g. ["MENTORSHIP","GYM_PARTNER"]
}
