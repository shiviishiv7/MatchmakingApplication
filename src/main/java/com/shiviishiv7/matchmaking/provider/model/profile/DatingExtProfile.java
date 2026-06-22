package com.shiviishiv7.matchmaking.provider.model.profile;

import com.shiviishiv7.matchmaking.provider.model.BaseEntity;
import com.shiviishiv7.matchmaking.provider.vo.DatingExtProfileVO;
import jakarta.persistence.*;
import lombok.*;

@Entity
@Table(name = "extDatingProfiles")
@Getter @Setter @NoArgsConstructor @AllArgsConstructor @Builder
public class DatingExtProfile extends BaseEntity {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    @Column(name = "id", updatable = false, nullable = false)
    private Integer id;

    @Column(name = "cognitoSub", nullable = false, unique = true)
    private String cognitoSub;

    @Column(name = "dietaryHabits", length = 30)
    private String dietaryHabits;

    @Column(name = "smokingHabit", length = 20)
    private String smokingHabit;

    @Column(name = "drinkingHabit", length = 20)
    private String drinkingHabit;

    @Column(name = "heightCm")
    private Integer heightCm;

    @Column(name = "bodyType", length = 30)
    private String bodyType;

    @Column(name = "relationshipGoal", length = 50)
    private String relationshipGoal;

    @Column(name = "sexualOrientation", length = 50)
    private String sexualOrientation;

    @Column(name = "hasChildren")
    private Boolean hasChildren;

    @Column(name = "wantsChildren")
    private Boolean wantsChildren;

    @Column(name = "loveLanguage", length = 50)
    private String loveLanguage;

    @Column(name = "personalityType", length = 20)
    private String personalityType;

    @Column(name = "interestTags", length = 500)
    private String interestTags;

    @Column(name = "promptQuestion1", length = 200)
    private String promptQuestion1;

    @Column(name = "promptAnswer1", columnDefinition = "TEXT")
    private String promptAnswer1;

    @Column(name = "promptQuestion2", length = 200)
    private String promptQuestion2;

    @Column(name = "promptAnswer2", columnDefinition = "TEXT")
    private String promptAnswer2;

    @Column(name = "prefAgeMin")
    private Integer prefAgeMin;

    @Column(name = "prefAgeMax")
    private Integer prefAgeMax;

    @Column(name = "prefGenders", length = 100)
    private String prefGenders;

    @Column(name = "prefHeightMinCm")
    private Integer prefHeightMinCm;

    @Column(name = "prefRelationshipGoal", length = 50)
    private String prefRelationshipGoal;

    public DatingExtProfile fromVO(DatingExtProfileVO vo) {
        if (vo == null) return null;
        this.setId(vo.getId());
        this.setCognitoSub(vo.getCognitoSub());
        this.setDietaryHabits(vo.getDietaryHabits());
        this.setSmokingHabit(vo.getSmokingHabit());
        this.setDrinkingHabit(vo.getDrinkingHabit());
        this.setHeightCm(vo.getHeightCm());
        this.setBodyType(vo.getBodyType());
        this.setRelationshipGoal(vo.getRelationshipGoal());
        this.setSexualOrientation(vo.getSexualOrientation());
        this.setHasChildren(vo.getHasChildren());
        this.setWantsChildren(vo.getWantsChildren());
        this.setLoveLanguage(vo.getLoveLanguage());
        this.setPersonalityType(vo.getPersonalityType());
        this.setInterestTags(vo.getInterestTags());
        this.setPromptQuestion1(vo.getPromptQuestion1());
        this.setPromptAnswer1(vo.getPromptAnswer1());
        this.setPromptQuestion2(vo.getPromptQuestion2());
        this.setPromptAnswer2(vo.getPromptAnswer2());
        this.setPrefAgeMin(vo.getPrefAgeMin());
        this.setPrefAgeMax(vo.getPrefAgeMax());
        this.setPrefGenders(vo.getPrefGenders());
        this.setPrefHeightMinCm(vo.getPrefHeightMinCm());
        this.setPrefRelationshipGoal(vo.getPrefRelationshipGoal());
        return this;
    }

    public DatingExtProfileVO toVO() {
        DatingExtProfileVO vo = new DatingExtProfileVO();
        vo.setId(this.getId());
        vo.setCognitoSub(this.getCognitoSub());
        vo.setDietaryHabits(this.getDietaryHabits());
        vo.setSmokingHabit(this.getSmokingHabit());
        vo.setDrinkingHabit(this.getDrinkingHabit());
        vo.setHeightCm(this.getHeightCm());
        vo.setBodyType(this.getBodyType());
        vo.setRelationshipGoal(this.getRelationshipGoal());
        vo.setSexualOrientation(this.getSexualOrientation());
        vo.setHasChildren(this.getHasChildren());
        vo.setWantsChildren(this.getWantsChildren());
        vo.setLoveLanguage(this.getLoveLanguage());
        vo.setPersonalityType(this.getPersonalityType());
        vo.setInterestTags(this.getInterestTags());
        vo.setPromptQuestion1(this.getPromptQuestion1());
        vo.setPromptAnswer1(this.getPromptAnswer1());
        vo.setPromptQuestion2(this.getPromptQuestion2());
        vo.setPromptAnswer2(this.getPromptAnswer2());
        vo.setPrefAgeMin(this.getPrefAgeMin());
        vo.setPrefAgeMax(this.getPrefAgeMax());
        vo.setPrefGenders(this.getPrefGenders());
        vo.setPrefHeightMinCm(this.getPrefHeightMinCm());
        vo.setPrefRelationshipGoal(this.getPrefRelationshipGoal());
        return vo;
    }
}
