package com.shiviishiv7.matchmaking.provider.vo;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import java.time.LocalDate;
import lombok.Getter;
import lombok.Setter;
import lombok.ToString;

@Getter
@Setter
@ToString
@JsonIgnoreProperties(ignoreUnknown = true)
public class FlatmateExtProfileVO {

    private Integer id;
    private String cognitoSub;
    private String lookingIn;
    private String budgetRangeInr;
    private LocalDate moveInDate;
    private String preferredFlatmateGender;
    private String occupationType;
    private Boolean isVegetarianHousehold;
    private Boolean allowsSmoking;
    private Boolean hasPets;
    private Boolean allowsPets;
    private String sleepSchedule;
    private String cleanlinessLevel;
    private String guestsPolicy;
    private Boolean hasCurrentFlat;
    private String currentFlatDetails;

    public boolean validate() {
        if (cognitoSub == null) throw new IllegalArgumentException("cognitoSub is required.");
        return true;
    }
}
