package com.shiviishiv7.matchmaking.provider.model;

import com.shiviishiv7.matchmaking.provider.vo.FitnessExtProfileVO;
import jakarta.persistence.*;
import lombok.*;

@Entity
@Table(name = "extFitnessProfiles")
@Getter @Setter @NoArgsConstructor @AllArgsConstructor @Builder
public class FitnessExtProfile extends BaseEntity {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    @Column(name = "id", updatable = false, nullable = false)
    private Integer id;

    @Column(name = "userId", nullable = false, unique = true)
    private Integer userId;

    @Column(name = "fitnessActivities", length = 300)
    private String fitnessActivities;

    @Column(name = "fitnessLevel", length = 30)
    private String fitnessLevel;

    @Column(name = "workoutDays", length = 100)
    private String workoutDays;

    @Column(name = "preferredWorkoutTime", length = 50)
    private String preferredWorkoutTime;

    @Column(name = "gymName", length = 150)
    private String gymName;

    @Column(name = "isOkWithMixedGender")
    private Boolean isOkWithMixedGender;

    @Column(name = "sportsLeagueLevel", length = 30)
    private String sportsLeagueLevel;

    @Column(name = "fitnessGoal", length = 100)
    private String fitnessGoal;

    @Column(name = "dietPreference", length = 50)
    private String dietPreference;

    public FitnessExtProfile fromVO(FitnessExtProfileVO vo) {
        if (vo == null) return null;
        this.setId(vo.getId());
        this.setUserId(vo.getUserId());
        this.setFitnessActivities(vo.getFitnessActivities());
        this.setFitnessLevel(vo.getFitnessLevel());
        this.setWorkoutDays(vo.getWorkoutDays());
        this.setPreferredWorkoutTime(vo.getPreferredWorkoutTime());
        this.setGymName(vo.getGymName());
        this.setIsOkWithMixedGender(vo.getIsOkWithMixedGender());
        this.setSportsLeagueLevel(vo.getSportsLeagueLevel());
        this.setFitnessGoal(vo.getFitnessGoal());
        this.setDietPreference(vo.getDietPreference());
        return this;
    }

    public FitnessExtProfileVO toVO() {
        FitnessExtProfileVO vo = new FitnessExtProfileVO();
        vo.setId(this.getId());
        vo.setUserId(this.getUserId());
        vo.setFitnessActivities(this.getFitnessActivities());
        vo.setFitnessLevel(this.getFitnessLevel());
        vo.setWorkoutDays(this.getWorkoutDays());
        vo.setPreferredWorkoutTime(this.getPreferredWorkoutTime());
        vo.setGymName(this.getGymName());
        vo.setIsOkWithMixedGender(this.getIsOkWithMixedGender());
        vo.setSportsLeagueLevel(this.getSportsLeagueLevel());
        vo.setFitnessGoal(this.getFitnessGoal());
        vo.setDietPreference(this.getDietPreference());
        return vo;
    }
}
