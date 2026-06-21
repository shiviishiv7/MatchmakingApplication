package com.shiviishiv7.matchmaking.provider.vo;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import java.math.BigDecimal;
import lombok.Getter;
import lombok.Setter;
import lombok.ToString;

@Getter
@Setter
@ToString
@JsonIgnoreProperties(ignoreUnknown = true)
public class PartnerPreferenceVO {

    private Integer id;
    private Integer ageMin;
    private Integer ageMax;
    private Integer heightMinCm;
    private Integer heightMaxCm;
    private String maritalStatusPref;
    private String preferredCountries;
    private String preferredStates;
    private Boolean openToRelocation;
    private String religionPref;
    private String castePref;
    private String motherTonguePref;
    private String dietaryPref;
    private String educationPref;
    private String employmentTypePref;
    private BigDecimal incomeMinInr;
    private BigDecimal incomeMaxInr;
    private String smokingPref;
    private String drinkingPref;
    private String bodyTypePref;
    private String familyTypePref;
    private String familyValuesPref;
    private String manglikPref;
    private Boolean horoscopeMatchRequired;
    private String aboutPartner;

    public boolean validate() {
        if (ageMin != null && ageMax != null && ageMin > ageMax) throw new IllegalArgumentException("ageMin cannot exceed ageMax.");
        if (heightMinCm != null && heightMaxCm != null && heightMinCm > heightMaxCm) throw new IllegalArgumentException("heightMinCm cannot exceed heightMaxCm.");
        if (incomeMinInr != null && incomeMaxInr != null && incomeMinInr.compareTo(incomeMaxInr) > 0) throw new IllegalArgumentException("incomeMinInr cannot exceed incomeMaxInr.");
        return true;
    }
}
