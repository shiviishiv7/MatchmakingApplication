package com.shiviishiv7.matchmaking.provider.model;

import com.shiviishiv7.matchmaking.provider.vo.PartnerPreferenceVO;
import jakarta.persistence.*;
import lombok.*;

import java.math.BigDecimal;

@Entity
@Table(name = "partnerPreferences")
@Getter @Setter @NoArgsConstructor @AllArgsConstructor @Builder
public class PartnerPreference extends BaseEntity {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    @Column(name = "id", updatable = false, nullable = false)
    private Integer id;

    @Column(name = "ageMin")
    private Integer ageMin;

    @Column(name = "ageMax")
    private Integer ageMax;

    @Column(name = "heightMinCm")
    private Integer heightMinCm;

    @Column(name = "heightMaxCm")
    private Integer heightMaxCm;

    @Column(name = "maritalStatusPref", length = 200)
    private String maritalStatusPref;

    @Column(name = "preferredCountries", length = 300)
    private String preferredCountries;

    @Column(name = "preferredStates", length = 300)
    private String preferredStates;

    @Column(name = "openToRelocation")
    @Builder.Default
    private Boolean openToRelocation = false;

    @Column(name = "religionPref", length = 200)
    private String religionPref;

    @Column(name = "castePref", length = 300)
    private String castePref;

    @Column(name = "motherTonguePref", length = 300)
    private String motherTonguePref;

    @Column(name = "dietaryPref", length = 200)
    private String dietaryPref;

    @Column(name = "educationPref", length = 300)
    private String educationPref;

    @Column(name = "employmentTypePref", length = 200)
    private String employmentTypePref;

    @Column(name = "incomeMinInr", precision = 15, scale = 2)
    private BigDecimal incomeMinInr;

    @Column(name = "incomeMaxInr", precision = 15, scale = 2)
    private BigDecimal incomeMaxInr;

    @Column(name = "smokingPref", length = 100)
    private String smokingPref;

    @Column(name = "drinkingPref", length = 100)
    private String drinkingPref;

    @Column(name = "bodyTypePref", length = 200)
    private String bodyTypePref;

    @Column(name = "familyTypePref", length = 100)
    private String familyTypePref;

    @Column(name = "familyValuesPref", length = 100)
    private String familyValuesPref;

    @Column(name = "manglikPref", length = 100)
    private String manglikPref;

    @Column(name = "horoscopeMatchRequired")
    @Builder.Default
    private Boolean horoscopeMatchRequired = false;

    @Column(name = "aboutPartner", columnDefinition = "TEXT")
    private String aboutPartner;

    public PartnerPreference fromVO(PartnerPreferenceVO vo) {
        if (vo == null) return null;
        this.setId(vo.getId());
        this.setAgeMin(vo.getAgeMin());
        this.setAgeMax(vo.getAgeMax());
        this.setHeightMinCm(vo.getHeightMinCm());
        this.setHeightMaxCm(vo.getHeightMaxCm());
        this.setMaritalStatusPref(vo.getMaritalStatusPref());
        this.setPreferredCountries(vo.getPreferredCountries());
        this.setPreferredStates(vo.getPreferredStates());
        this.setOpenToRelocation(vo.getOpenToRelocation());
        this.setReligionPref(vo.getReligionPref());
        this.setCastePref(vo.getCastePref());
        this.setMotherTonguePref(vo.getMotherTonguePref());
        this.setDietaryPref(vo.getDietaryPref());
        this.setEducationPref(vo.getEducationPref());
        this.setEmploymentTypePref(vo.getEmploymentTypePref());
        this.setIncomeMinInr(vo.getIncomeMinInr());
        this.setIncomeMaxInr(vo.getIncomeMaxInr());
        this.setSmokingPref(vo.getSmokingPref());
        this.setDrinkingPref(vo.getDrinkingPref());
        this.setBodyTypePref(vo.getBodyTypePref());
        this.setFamilyTypePref(vo.getFamilyTypePref());
        this.setFamilyValuesPref(vo.getFamilyValuesPref());
        this.setManglikPref(vo.getManglikPref());
        this.setHoroscopeMatchRequired(vo.getHoroscopeMatchRequired());
        this.setAboutPartner(vo.getAboutPartner());
        return this;
    }

    public PartnerPreferenceVO toVO() {
        PartnerPreferenceVO vo = new PartnerPreferenceVO();
        vo.setId(this.getId());
        vo.setAgeMin(this.getAgeMin());
        vo.setAgeMax(this.getAgeMax());
        vo.setHeightMinCm(this.getHeightMinCm());
        vo.setHeightMaxCm(this.getHeightMaxCm());
        vo.setMaritalStatusPref(this.getMaritalStatusPref());
        vo.setPreferredCountries(this.getPreferredCountries());
        vo.setPreferredStates(this.getPreferredStates());
        vo.setOpenToRelocation(this.getOpenToRelocation());
        vo.setReligionPref(this.getReligionPref());
        vo.setCastePref(this.getCastePref());
        vo.setMotherTonguePref(this.getMotherTonguePref());
        vo.setDietaryPref(this.getDietaryPref());
        vo.setEducationPref(this.getEducationPref());
        vo.setEmploymentTypePref(this.getEmploymentTypePref());
        vo.setIncomeMinInr(this.getIncomeMinInr());
        vo.setIncomeMaxInr(this.getIncomeMaxInr());
        vo.setSmokingPref(this.getSmokingPref());
        vo.setDrinkingPref(this.getDrinkingPref());
        vo.setBodyTypePref(this.getBodyTypePref());
        vo.setFamilyTypePref(this.getFamilyTypePref());
        vo.setFamilyValuesPref(this.getFamilyValuesPref());
        vo.setManglikPref(this.getManglikPref());
        vo.setHoroscopeMatchRequired(this.getHoroscopeMatchRequired());
        vo.setAboutPartner(this.getAboutPartner());
        return vo;
    }
}
