package com.shiviishiv7.matchmaking.provider.vo;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import com.shiviishiv7.matchmaking.common.enums.Gender;
import com.shiviishiv7.matchmaking.provider.model.UserPreference;
import lombok.Getter;
import lombok.Setter;

import java.util.List;
import java.util.UUID;

@Getter
@Setter
@JsonIgnoreProperties(ignoreUnknown = true)
public class UserPreferenceVO {

    private UUID id;
    private UUID userId;
    private Integer minAge;
    private Integer maxAge;
    private Gender preferredGender;
    private List<String> preferredIndustries;
    private Integer maxTimezoneOffsetHours;
    private Boolean sameCompanyAllowed;
    private String location;

    public boolean validate() {
        if (userId == null) {
            throw new IllegalArgumentException("User ID cannot be null");
        }
        if (minAge != null && maxAge != null && minAge > maxAge) {
            throw new IllegalArgumentException("Min age cannot be greater than max age");
        }
        if (maxTimezoneOffsetHours != null && maxTimezoneOffsetHours < 0) {
            throw new IllegalArgumentException("Max timezone offset hours cannot be negative");
        }
        return true;
    }

    public UserPreference fromVO() {
        UserPreference preference = new UserPreference();
        preference.setId(id);
        preference.setMinAge(minAge);
        preference.setMaxAge(maxAge);
        preference.setPreferredGender(preferredGender);
        preference.setPreferredIndustries(preferredIndustries);
        preference.setMaxTimezoneOffsetHours(maxTimezoneOffsetHours);
        preference.setSameCompanyAllowed(sameCompanyAllowed);
        return preference;
    }

    public UserPreferenceVO toVO(UserPreference preference) {
        UserPreferenceVO vo = new UserPreferenceVO();
        vo.setId(preference.getId());
        vo.setUserId(preference.getUser() != null ? preference.getUser().getId() : null);
        vo.setMinAge(preference.getMinAge());
        vo.setMaxAge(preference.getMaxAge());
        vo.setPreferredGender(preference.getPreferredGender());
        vo.setPreferredIndustries(preference.getPreferredIndustries());
        vo.setMaxTimezoneOffsetHours(preference.getMaxTimezoneOffsetHours());
        vo.setSameCompanyAllowed(preference.getSameCompanyAllowed());
        return vo;
    }
}
