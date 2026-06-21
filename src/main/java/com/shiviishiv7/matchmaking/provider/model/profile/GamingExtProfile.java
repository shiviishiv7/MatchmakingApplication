package com.shiviishiv7.matchmaking.provider.model.profile;

import com.shiviishiv7.matchmaking.provider.model.BaseEntity;
import com.shiviishiv7.matchmaking.provider.vo.GamingExtProfileVO;
import jakarta.persistence.*;
import lombok.*;

@Entity
@Table(name = "extGamingProfiles")
@Getter @Setter @NoArgsConstructor @AllArgsConstructor @Builder
public class GamingExtProfile extends BaseEntity {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    @Column(name = "id", updatable = false, nullable = false)
    private Integer id;

    @Column(name = "userId", nullable = false, unique = true)
    private Integer userId;

    @Column(name = "platforms", length = 100)
    private String platforms;

    @Column(name = "favoriteGames", length = 500)
    private String favoriteGames;

    @Column(name = "favoriteGenres", length = 200)
    private String favoriteGenres;

    @Column(name = "gamingSchedule", length = 100)
    private String gamingSchedule;

    @Column(name = "skillLevel", length = 30)
    private String skillLevel;

    @Column(name = "communicationStyle", length = 30)
    private String communicationStyle;

    @Column(name = "isOkWithNewbies")
    private Boolean isOkWithNewbies;

    @Column(name = "gamertags", columnDefinition = "JSON")
    private String gamertags;

    public GamingExtProfile fromVO(GamingExtProfileVO vo) {
        if (vo == null) return null;
        this.setId(vo.getId());
        this.setUserId(vo.getUserId());
        this.setPlatforms(vo.getPlatforms());
        this.setFavoriteGames(vo.getFavoriteGames());
        this.setFavoriteGenres(vo.getFavoriteGenres());
        this.setGamingSchedule(vo.getGamingSchedule());
        this.setSkillLevel(vo.getSkillLevel());
        this.setCommunicationStyle(vo.getCommunicationStyle());
        this.setIsOkWithNewbies(vo.getIsOkWithNewbies());
        this.setGamertags(vo.getGamertags());
        return this;
    }

    public GamingExtProfileVO toVO() {
        GamingExtProfileVO vo = new GamingExtProfileVO();
        vo.setId(this.getId());
        vo.setUserId(this.getUserId());
        vo.setPlatforms(this.getPlatforms());
        vo.setFavoriteGames(this.getFavoriteGames());
        vo.setFavoriteGenres(this.getFavoriteGenres());
        vo.setGamingSchedule(this.getGamingSchedule());
        vo.setSkillLevel(this.getSkillLevel());
        vo.setCommunicationStyle(this.getCommunicationStyle());
        vo.setIsOkWithNewbies(this.getIsOkWithNewbies());
        vo.setGamertags(this.getGamertags());
        return vo;
    }
}
