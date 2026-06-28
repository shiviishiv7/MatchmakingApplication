package com.shiviishiv7.matchmaking.provider.model;

import com.shiviishiv7.matchmaking.common.enums.*;
import com.shiviishiv7.matchmaking.provider.vo.PartnerPreferenceVO;
import jakarta.persistence.*;
import lombok.*;

import java.math.BigDecimal;

@Entity
@Table(name = "PARTNER_PREFERENCES", indexes = {
        @Index(name = "idx_partner_pref_post_id", columnList = "postId"),
        @Index(name = "idx_partner_pref_cognito_sub", columnList = "cognitoSub")
})
@Getter @Setter @NoArgsConstructor @AllArgsConstructor @Builder
public class PartnerPreference extends BaseEntity {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    @Column(name = "id", updatable = false, nullable = false)
    private Integer id;

    @Column(name = "postId")
    private Long postId;

    @Column(name = "cognitoSub")
    private String cognitoSub;

    @Enumerated(EnumType.STRING)
    @Column(name = "intent", length = 20)
    private IntentType intent;

    @Column(name = "ageMin")
    private Integer ageMin;

    @Column(name = "ageMax")
    private Integer ageMax;

    @Column(name = "heightMinCm")
    private Integer heightMinCm;

    @Column(name = "heightMaxCm")
    private Integer heightMaxCm;

    @Enumerated(EnumType.STRING)
    @Column(name = "genderPref", length = 20)
    private Gender genderPref;

    @Enumerated(EnumType.STRING)
    @Column(name = "maritalStatusPref", length = 30)
    private MaritalStatus maritalStatusPref;

    @Column(name = "preferredStates", length = 300)
    private String preferredStates;

    @Column(name = "openToRelocation")
    @Builder.Default
    private Boolean openToRelocation = false;

    @Enumerated(EnumType.STRING)
    @Column(name = "religionPref", length = 30)
    private Religion religionPref;

    @Enumerated(EnumType.STRING)
    @Column(name = "motherTonguePref", length = 30)
    private Language motherTonguePref;

    @Enumerated(EnumType.STRING)
    @Column(name = "dietaryPref", length = 30)
    private DietPreference dietaryPref;

    @Enumerated(EnumType.STRING)
    @Column(name = "educationPref", length = 30)
    private Qualification educationPref;

    @Enumerated(EnumType.STRING)
    @Column(name = "employmentTypePref", length = 40)
    private Profession employmentTypePref;

    @Column(name = "incomeMinInr", precision = 15, scale = 2)
    private BigDecimal incomeMinInr;

    @Column(name = "incomeMaxInr", precision = 15, scale = 2)
    private BigDecimal incomeMaxInr;

    @Enumerated(EnumType.STRING)
    @Column(name = "smokingPref", length = 20)
    private SmokingHabit smokingPref;

    @Enumerated(EnumType.STRING)
    @Column(name = "drinkingPref", length = 20)
    private DrinkingHabit drinkingPref;

    @Enumerated(EnumType.STRING)
    @Column(name = "familyTypePref", length = 20)
    private FamilyType familyTypePref;

    @Enumerated(EnumType.STRING)
    @Column(name = "familyValuesPref", length = 20)
    private FamilyValues familyValuesPref;

    @Enumerated(EnumType.STRING)
    @Column(name = "marriageTimelinePref", length = 20)
    private MarriageTimeline marriageTimelinePref;

    @Column(name = "okWithPartnerWorkingPref")
    private Boolean okWithPartnerWorkingPref;

    @Enumerated(EnumType.STRING)
    @Column(name = "relationshipGoalPref", length = 30)
    private RelationshipGoal relationshipGoalPref;

    @Column(name = "aboutPartner", columnDefinition = "TEXT")
    private String aboutPartner;

    public PartnerPreference fromVO(PartnerPreferenceVO vo) {
        if (vo == null) return null;
        this.setId(vo.getId());
        this.setPostId(vo.getPostId());
        this.setCognitoSub(vo.getCognitoSub());
        this.setIntent(vo.getIntent());
        this.setAgeMin(vo.getAgeMin());
        this.setAgeMax(vo.getAgeMax());
        this.setHeightMinCm(vo.getHeightMinCm());
        this.setHeightMaxCm(vo.getHeightMaxCm());
        this.setGenderPref(vo.getGenderPref());
        this.setMaritalStatusPref(vo.getMaritalStatusPref());
        this.setPreferredStates(vo.getPreferredStates());
        this.setOpenToRelocation(vo.getOpenToRelocation());
        this.setReligionPref(vo.getReligionPref());
        this.setMotherTonguePref(vo.getMotherTonguePref());
        this.setDietaryPref(vo.getDietaryPref());
        this.setEducationPref(vo.getEducationPref());
        this.setEmploymentTypePref(vo.getEmploymentTypePref());
        this.setIncomeMinInr(vo.getIncomeMinInr());
        this.setIncomeMaxInr(vo.getIncomeMaxInr());
        this.setSmokingPref(vo.getSmokingPref());
        this.setDrinkingPref(vo.getDrinkingPref());
        this.setFamilyTypePref(vo.getFamilyTypePref());
        this.setFamilyValuesPref(vo.getFamilyValuesPref());
//        this.setWantsChildrenPref(vo.getWantsChildrenPref());
        this.setMarriageTimelinePref(vo.getMarriageTimelinePref());
        this.setOkWithPartnerWorkingPref(vo.getOkWithPartnerWorkingPref());
        this.setRelationshipGoalPref(vo.getRelationshipGoalPref());
        this.setAboutPartner(vo.getAboutPartner());
        return this;
    }

    public PartnerPreferenceVO toVO() {
        PartnerPreferenceVO vo = new PartnerPreferenceVO();
        vo.setId(this.getId());
        vo.setPostId(this.getPostId());
        vo.setCognitoSub(this.getCognitoSub());
        vo.setIntent(this.getIntent());
        vo.setAgeMin(this.getAgeMin());
        vo.setAgeMax(this.getAgeMax());
        vo.setHeightMinCm(this.getHeightMinCm());
        vo.setHeightMaxCm(this.getHeightMaxCm());
        vo.setGenderPref(this.getGenderPref());
        vo.setMaritalStatusPref(this.getMaritalStatusPref());
        vo.setPreferredStates(this.getPreferredStates());
        vo.setOpenToRelocation(this.getOpenToRelocation());
        vo.setReligionPref(this.getReligionPref());
        vo.setMotherTonguePref(this.getMotherTonguePref());
        vo.setDietaryPref(this.getDietaryPref());
        vo.setEducationPref(this.getEducationPref());
        vo.setEmploymentTypePref(this.getEmploymentTypePref());
        vo.setIncomeMinInr(this.getIncomeMinInr());
        vo.setIncomeMaxInr(this.getIncomeMaxInr());
        vo.setSmokingPref(this.getSmokingPref());
        vo.setDrinkingPref(this.getDrinkingPref());
        vo.setFamilyTypePref(this.getFamilyTypePref());
        vo.setFamilyValuesPref(this.getFamilyValuesPref());
//        vo.setWantsChildrenPref(this.getWantsChildrenPref());
        vo.setMarriageTimelinePref(this.getMarriageTimelinePref());
        vo.setOkWithPartnerWorkingPref(this.getOkWithPartnerWorkingPref());
        vo.setRelationshipGoalPref(this.getRelationshipGoalPref());
        vo.setAboutPartner(this.getAboutPartner());
        return vo;
    }
}
