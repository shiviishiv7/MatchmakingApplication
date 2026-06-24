package com.shiviishiv7.matchmaking.provider.vo.ws;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import lombok.Getter;
import lombok.Setter;

/**
 * Payload for /app/webrtc.search.
 *
 * If childCategory is present → Advanced search (category + basic fields).
 * If childCategory is absent  → Basic search (gender + city + age only).
 */
@Getter
@Setter
@JsonIgnoreProperties(ignoreUnknown = true)
public class InstantSearchFilterVO {

    // Presence of this field switches to advanced search
    private String childCategory;

    // Basic filter fields (both tiers)
    private String preferredGender;
    private String preferredCity;
    private Integer minAge;
    private Integer maxAge;
}
