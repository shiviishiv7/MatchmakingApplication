package com.shiviishiv7.matchmaking.provider.vo;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import lombok.Getter;
import lombok.Setter;
import lombok.ToString;

@Getter
@Setter
@ToString
@JsonIgnoreProperties(ignoreUnknown = true)
public class GamingExtProfileVO {

    private Integer id;
    private String cognitoSub;
    private String platforms;
    private String favoriteGames;
    private String favoriteGenres;
    private String gamingSchedule;
    private String skillLevel;
    private String communicationStyle;
    private Boolean isOkWithNewbies;
    private String gamertags;

    public boolean validate() {
        if (cognitoSub == null) throw new IllegalArgumentException("cognitoSub is required.");
        return true;
    }
}
