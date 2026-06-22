package com.shiviishiv7.matchmaking.provider.vo;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import com.shiviishiv7.matchmaking.common.enums.MatchCategory;
import java.time.LocalDateTime;
import lombok.Getter;
import lombok.Setter;
import lombok.ToString;

@Getter
@Setter
@ToString
@JsonIgnoreProperties(ignoreUnknown = true)
public class CategoryProfileRegistryVO {

    private Integer id;
    private String cognitoSub;
    private MatchCategory matchCategory;

    // Common filter fields
    private String preferredGender;
    private String preferredCity;
    private String preferredState;
    private String preferredCountry;
    private Integer maxTimezoneOffsetHours;
    private Boolean sameCompanyAllowed;

    private Integer completionPct;
    private Boolean isActive;
    private LocalDateTime createdAt;

    public boolean validate() {
        if (cognitoSub == null) throw new IllegalArgumentException("cognitoSub is required.");
        if (matchCategory == null) throw new IllegalArgumentException("matchCategory is required.");
        return true;
    }
}
