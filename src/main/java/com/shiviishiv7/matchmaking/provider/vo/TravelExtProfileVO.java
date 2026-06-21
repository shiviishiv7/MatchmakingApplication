package com.shiviishiv7.matchmaking.provider.vo;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;

import lombok.Getter;
import lombok.Setter;
import lombok.ToString;

@Getter
@Setter
@ToString
@JsonIgnoreProperties(ignoreUnknown = true)
public class TravelExtProfileVO {

    private Integer id;
    private Integer userId;
    private String travelStyle;
    private String preferredDestinations;
    private String bucketListPlaces;
    private Integer tripsPerYear;
    private String preferredTripDuration;
    private Boolean hasTraveledAbroad;
    private Integer countriesVisited;
    private String dietaryNeeds;
    private Boolean isOkWithBudgetStays;
    private Boolean isOkWithCamping;
    private String preferredGroupSize;
    private String upcomingTrips;
    private String pastTripsHighlights;

    public boolean validate() {
        if (userId == null) throw new IllegalArgumentException("userId is required.");
        return true;
    }
}
