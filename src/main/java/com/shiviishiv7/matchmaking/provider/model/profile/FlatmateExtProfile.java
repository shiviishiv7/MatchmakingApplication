package com.shiviishiv7.matchmaking.provider.model.profile;

import com.shiviishiv7.matchmaking.provider.model.BaseEntity;
import com.shiviishiv7.matchmaking.provider.vo.FlatmateExtProfileVO;
import jakarta.persistence.*;
import lombok.*;

import java.time.LocalDate;

@Entity
@Table(name = "extFlatmateProfiles")
@Getter @Setter @NoArgsConstructor @AllArgsConstructor @Builder
public class FlatmateExtProfile extends BaseEntity {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    @Column(name = "id", updatable = false, nullable = false)
    private Integer id;

    @Column(name = "cognitoSub", nullable = false, unique = true)
    private String cognitoSub;

    @Column(name = "lookingIn", length = 200)
    private String lookingIn;

    @Column(name = "budgetRangeInr", length = 50)
    private String budgetRangeInr;

    @Column(name = "moveInDate")
    private LocalDate moveInDate;

    @Column(name = "preferredFlatmateGender", length = 20)
    private String preferredFlatmateGender;

    @Column(name = "occupationType", length = 50)
    private String occupationType;

    @Column(name = "isVegetarianHousehold")
    private Boolean isVegetarianHousehold;

    @Column(name = "allowsSmoking")
    private Boolean allowsSmoking;

    @Column(name = "hasPets")
    private Boolean hasPets;

    @Column(name = "allowsPets")
    private Boolean allowsPets;

    @Column(name = "sleepSchedule", length = 30)
    private String sleepSchedule;

    @Column(name = "cleanlinessLevel", length = 30)
    private String cleanlinessLevel;

    @Column(name = "guestsPolicy", length = 30)
    private String guestsPolicy;

    @Column(name = "hasCurrentFlat")
    private Boolean hasCurrentFlat;

    @Column(name = "currentFlatDetails", columnDefinition = "TEXT")
    private String currentFlatDetails;

    public FlatmateExtProfile fromVO(FlatmateExtProfileVO vo) {
        if (vo == null) return null;
        this.setId(vo.getId());
        this.setCognitoSub(vo.getCognitoSub());
        this.setLookingIn(vo.getLookingIn());
        this.setBudgetRangeInr(vo.getBudgetRangeInr());
        this.setMoveInDate(vo.getMoveInDate());
        this.setPreferredFlatmateGender(vo.getPreferredFlatmateGender());
        this.setOccupationType(vo.getOccupationType());
        this.setIsVegetarianHousehold(vo.getIsVegetarianHousehold());
        this.setAllowsSmoking(vo.getAllowsSmoking());
        this.setHasPets(vo.getHasPets());
        this.setAllowsPets(vo.getAllowsPets());
        this.setSleepSchedule(vo.getSleepSchedule());
        this.setCleanlinessLevel(vo.getCleanlinessLevel());
        this.setGuestsPolicy(vo.getGuestsPolicy());
        this.setHasCurrentFlat(vo.getHasCurrentFlat());
        this.setCurrentFlatDetails(vo.getCurrentFlatDetails());
        return this;
    }

    public FlatmateExtProfileVO toVO() {
        FlatmateExtProfileVO vo = new FlatmateExtProfileVO();
        vo.setId(this.getId());
        vo.setCognitoSub(this.getCognitoSub());
        vo.setLookingIn(this.getLookingIn());
        vo.setBudgetRangeInr(this.getBudgetRangeInr());
        vo.setMoveInDate(this.getMoveInDate());
        vo.setPreferredFlatmateGender(this.getPreferredFlatmateGender());
        vo.setOccupationType(this.getOccupationType());
        vo.setIsVegetarianHousehold(this.getIsVegetarianHousehold());
        vo.setAllowsSmoking(this.getAllowsSmoking());
        vo.setHasPets(this.getHasPets());
        vo.setAllowsPets(this.getAllowsPets());
        vo.setSleepSchedule(this.getSleepSchedule());
        vo.setCleanlinessLevel(this.getCleanlinessLevel());
        vo.setGuestsPolicy(this.getGuestsPolicy());
        vo.setHasCurrentFlat(this.getHasCurrentFlat());
        vo.setCurrentFlatDetails(this.getCurrentFlatDetails());
        return vo;
    }
}
