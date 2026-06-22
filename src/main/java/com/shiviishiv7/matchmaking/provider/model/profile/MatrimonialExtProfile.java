package com.shiviishiv7.matchmaking.provider.model.profile;

import com.shiviishiv7.matchmaking.provider.model.BaseEntity;
import com.shiviishiv7.matchmaking.provider.model.PartnerPreference;
import com.shiviishiv7.matchmaking.provider.vo.MatrimonialExtProfileVO;
import jakarta.persistence.*;
import lombok.*;

import java.math.BigDecimal;
import java.time.LocalTime;

@Entity
@Table(name = "EXT_MATRIMONIAL_PROFILES")
@Getter @Setter @NoArgsConstructor @AllArgsConstructor @Builder
public class MatrimonialExtProfile extends BaseEntity {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    @Column(name = "id", updatable = false, nullable = false)
    private Integer id;

    @Column(name = "cognitoSub", nullable = false, unique = true)
    private String cognitoSub;

    @Column(name = "religion", length = 50)
    private String religion;

    @Column(name = "caste", length = 100)
    private String caste;

    @Column(name = "subCaste", length = 100)
    private String subCaste;

    @Column(name = "gotram", length = 100)
    private String gotram;

    @Column(name = "motherTongue", length = 60)
    private String motherTongue;

    @Column(name = "dietaryHabits", length = 30)
    private String dietaryHabits;

    @Column(name = "highestEducation", length = 100)
    private String highestEducation;

    @Column(name = "educationDetail", length = 200)
    private String educationDetail;

    @Column(name = "profession", length = 100)
    private String profession;

    @Column(name = "employerName", length = 150)
    private String employerName;

    @Column(name = "employmentType", length = 40)
    private String employmentType;

    @Column(name = "annualIncomeInr", precision = 15, scale = 2)
    private BigDecimal annualIncomeInr;

    @Column(name = "nativeCity", length = 100)
    private String nativeCity;

    @Column(name = "nativeState", length = 100)
    private String nativeState;

    @Column(name = "familyType", length = 30)
    private String familyType;

    @Column(name = "familyValues", length = 30)
    private String familyValues;

    @Column(name = "familyStatus", length = 40)
    private String familyStatus;

    @Column(name = "fatherOccupation", length = 150)
    private String fatherOccupation;

    @Column(name = "motherOccupation", length = 150)
    private String motherOccupation;

    @Column(name = "siblingsCount")
    private Integer siblingsCount;

    @Column(name = "siblingsDetail", length = 300)
    private String siblingsDetail;

    @Column(name = "heightCm")
    private Integer heightCm;

    @Column(name = "maritalStatus", length = 30)
    private String maritalStatus;

    @Column(name = "bodyType", length = 30)
    private String bodyType;

    @Column(name = "complexion", length = 30)
    private String complexion;

    @Column(name = "smokingHabit", length = 20)
    private String smokingHabit;

    @Column(name = "drinkingHabit", length = 20)
    private String drinkingHabit;

    @Column(name = "manglikStatus", length = 20)
    private String manglikStatus;

    @Column(name = "rashi", length = 50)
    private String rashi;

    @Column(name = "nakshatra", length = 60)
    private String nakshatra;

    @Column(name = "birthPlace", length = 150)
    private String birthPlace;

    @Column(name = "birthTime")
    private LocalTime birthTime;

    @Column(name = "horoscopeMatchRequired")
    @Builder.Default
    private Boolean horoscopeMatchRequired = false;

    @OneToOne(cascade = CascadeType.ALL, orphanRemoval = true)
    @JoinColumn(name = "partnerPrefId")
    private PartnerPreference partnerPreference;

    public MatrimonialExtProfile fromVO(MatrimonialExtProfileVO vo) {
        if (vo == null) return null;
        this.setId(vo.getId());
        this.setCognitoSub(vo.getCognitoSub());
        this.setReligion(vo.getReligion());
        this.setCaste(vo.getCaste());
        this.setSubCaste(vo.getSubCaste());
        this.setGotram(vo.getGotram());
        this.setMotherTongue(vo.getMotherTongue());
        this.setDietaryHabits(vo.getDietaryHabits());
        this.setHighestEducation(vo.getHighestEducation());
        this.setEducationDetail(vo.getEducationDetail());
        this.setProfession(vo.getProfession());
        this.setEmployerName(vo.getEmployerName());
        this.setEmploymentType(vo.getEmploymentType());
        this.setAnnualIncomeInr(vo.getAnnualIncomeInr());
        this.setNativeCity(vo.getNativeCity());
        this.setNativeState(vo.getNativeState());
        this.setFamilyType(vo.getFamilyType());
        this.setFamilyValues(vo.getFamilyValues());
        this.setFamilyStatus(vo.getFamilyStatus());
        this.setFatherOccupation(vo.getFatherOccupation());
        this.setMotherOccupation(vo.getMotherOccupation());
        this.setSiblingsCount(vo.getSiblingsCount());
        this.setSiblingsDetail(vo.getSiblingsDetail());
        this.setHeightCm(vo.getHeightCm());
        this.setMaritalStatus(vo.getMaritalStatus());
        this.setBodyType(vo.getBodyType());
        this.setComplexion(vo.getComplexion());
        this.setSmokingHabit(vo.getSmokingHabit());
        this.setDrinkingHabit(vo.getDrinkingHabit());
        this.setManglikStatus(vo.getManglikStatus());
        this.setRashi(vo.getRashi());
        this.setNakshatra(vo.getNakshatra());
        this.setBirthPlace(vo.getBirthPlace());
        this.setBirthTime(vo.getBirthTime());
        this.setHoroscopeMatchRequired(vo.getHoroscopeMatchRequired());
        return this;
    }

    public MatrimonialExtProfileVO toVO() {
        MatrimonialExtProfileVO vo = new MatrimonialExtProfileVO();
        vo.setId(this.getId());
        vo.setCognitoSub(this.getCognitoSub());
        vo.setReligion(this.getReligion());
        vo.setCaste(this.getCaste());
        vo.setSubCaste(this.getSubCaste());
        vo.setGotram(this.getGotram());
        vo.setMotherTongue(this.getMotherTongue());
        vo.setDietaryHabits(this.getDietaryHabits());
        vo.setHighestEducation(this.getHighestEducation());
        vo.setEducationDetail(this.getEducationDetail());
        vo.setProfession(this.getProfession());
        vo.setEmployerName(this.getEmployerName());
        vo.setEmploymentType(this.getEmploymentType());
        vo.setAnnualIncomeInr(this.getAnnualIncomeInr());
        vo.setNativeCity(this.getNativeCity());
        vo.setNativeState(this.getNativeState());
        vo.setFamilyType(this.getFamilyType());
        vo.setFamilyValues(this.getFamilyValues());
        vo.setFamilyStatus(this.getFamilyStatus());
        vo.setFatherOccupation(this.getFatherOccupation());
        vo.setMotherOccupation(this.getMotherOccupation());
        vo.setSiblingsCount(this.getSiblingsCount());
        vo.setSiblingsDetail(this.getSiblingsDetail());
        vo.setHeightCm(this.getHeightCm());
        vo.setMaritalStatus(this.getMaritalStatus());
        vo.setBodyType(this.getBodyType());
        vo.setComplexion(this.getComplexion());
        vo.setSmokingHabit(this.getSmokingHabit());
        vo.setDrinkingHabit(this.getDrinkingHabit());
        vo.setManglikStatus(this.getManglikStatus());
        vo.setRashi(this.getRashi());
        vo.setNakshatra(this.getNakshatra());
        vo.setBirthPlace(this.getBirthPlace());
        vo.setBirthTime(this.getBirthTime());
        vo.setHoroscopeMatchRequired(this.getHoroscopeMatchRequired());
        return vo;
    }
}
