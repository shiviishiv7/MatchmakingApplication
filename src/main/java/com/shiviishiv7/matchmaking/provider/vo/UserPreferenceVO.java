package com.shiviishiv7.matchmaking.provider.vo;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import com.shiviishiv7.matchmaking.common.enums.DietaryHabit;
import com.shiviishiv7.matchmaking.common.enums.Gender;
import com.shiviishiv7.matchmaking.common.enums.Language;
import com.shiviishiv7.matchmaking.provider.model.UserPreference;
import jakarta.persistence.Column;
import lombok.Getter;
import lombok.Setter;

import java.util.List;


@Getter
@Setter
@JsonIgnoreProperties(ignoreUnknown = true)
public class UserPreferenceVO {

    private Integer id;

    private String cognitoSub;


    // ── Age ───────────────────────────────────────────────────────────────
    private Integer minAge;
    private Integer maxAge;
    private Integer minHeight;
    private Integer maxHeight;
    private List<DietaryHabit> dietaryHabit;
    private List<Language> language;

    // ── Gender ────────────────────────────────────────────────────────────
    private Gender preferredGender;

    // ── Industry ──────────────────────────────────────────────────────────
    private List<String> preferredIndustries; // matrimonia/dating/education/preparation
    private String profession;

    // ── Timezone ──────────────────────────────────────────────────────────
    private Integer maxTimezoneOffsetHours;

    // ── Company ───────────────────────────────────────────────────────────
    private Boolean sameCompanyAllowed;
    private List<String> preferredCompany;

    // ── Education ─────────────────────────────────────────────────────────
    private List<String> preferredCollege;

    // ── Location ──────────────────────────────────────────────────────────
    private String preferredZip;
    private String preferredCity;
    private String preferredState;
    private String preferredCountry;

    public boolean validate() {
        if (cognitoSub == null) {
            throw new IllegalArgumentException("CognitoSub ID cannot be null");
        }
        if (minAge != null && maxAge != null && minAge > maxAge) {
            throw new IllegalArgumentException("Min age cannot be greater than max age");
        }
//        if (maxTimezoneOffsetHours != null && maxTimezoneOffsetHours < 0) {
//            throw new IllegalArgumentException("Max timezone offset hours cannot be negative");
//        }
        return true;
    }

    public UserPreference fromVO() {
        UserPreference preference = new UserPreference();
        preference.setId(id);
        preference.setCognitoSub(cognitoSub);
        preference.setMinAge(minAge);
        preference.setMaxAge(maxAge);
        preference.setPreferredGender(preferredGender);
//        preference.setPreferredIndustries(preferredIndustries);
//        preference.setMaxTimezoneOffsetHours(maxTimezoneOffsetHours);
        preference.setSameCompanyAllowed(sameCompanyAllowed);
        preference.setPreferredCompany(preferredCompany);
        preference.setPreferredCollege(preferredCollege);
        preference.setPreferredZip(preferredZip);
        preference.setPreferredCity(preferredCity);
        preference.setPreferredState(preferredState);
        preference.setPreferredCountry(preferredCountry);
        return preference;
    }

    public UserPreferenceVO toVO(UserPreference preference) {
        UserPreferenceVO vo = new UserPreferenceVO();
        vo.setId(preference.getId());
        vo.setCognitoSub(preference.getCognitoSub());

        vo.setMinAge(preference.getMinAge());
        vo.setMaxAge(preference.getMaxAge());
        vo.setPreferredGender(preference.getPreferredGender());
//        vo.setPreferredIndustries(preference.getPreferredIndustries());
//        vo.setMaxTimezoneOffsetHours(preference.getMaxTimezoneOffsetHours());
        vo.setSameCompanyAllowed(preference.getSameCompanyAllowed());
        vo.setPreferredCompany(preference.getPreferredCompany());
        vo.setPreferredCollege(preference.getPreferredCollege());
        vo.setPreferredZip(preference.getPreferredZip());
        vo.setPreferredCity(preference.getPreferredCity());
        vo.setPreferredState(preference.getPreferredState());
        vo.setPreferredCountry(preference.getPreferredCountry());
        return vo;
    }
}
