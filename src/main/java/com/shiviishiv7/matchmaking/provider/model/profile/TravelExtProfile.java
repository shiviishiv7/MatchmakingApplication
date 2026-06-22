package com.shiviishiv7.matchmaking.provider.model.profile;

import com.shiviishiv7.matchmaking.provider.model.BaseEntity;
import com.shiviishiv7.matchmaking.provider.vo.TravelExtProfileVO;
import jakarta.persistence.*;
import lombok.*;

@Entity
@Table(name = "EXT_TRAVEL_PROFILES")
@Getter @Setter @NoArgsConstructor @AllArgsConstructor @Builder
public class TravelExtProfile extends BaseEntity {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    @Column(name = "id", updatable = false, nullable = false)
    private Integer id;

    @Column(name = "cognitoSub", nullable = false, unique = true)
    private String cognitoSub;

    @Column(name = "travelStyle", length = 50)
    private String travelStyle;

    @Column(name = "preferredDestinations", length = 500)
    private String preferredDestinations;

    @Column(name = "bucketListPlaces", length = 500)
    private String bucketListPlaces;

    @Column(name = "tripsPerYear")
    private Integer tripsPerYear;

    @Column(name = "preferredTripDuration", length = 50)
    private String preferredTripDuration;

    @Column(name = "hasTraveledAbroad")
    private Boolean hasTraveledAbroad;

    @Column(name = "countriesVisited")
    private Integer countriesVisited;

    @Column(name = "dietaryNeeds", length = 100)
    private String dietaryNeeds;

    @Column(name = "isOkWithBudgetStays")
    private Boolean isOkWithBudgetStays;

    @Column(name = "isOkWithCamping")
    private Boolean isOkWithCamping;

    @Column(name = "preferredGroupSize", length = 30)
    private String preferredGroupSize;

    @Column(name = "upcomingTrips", columnDefinition = "JSON")
    private String upcomingTrips;

    @Column(name = "pastTripsHighlights", columnDefinition = "TEXT")
    private String pastTripsHighlights;

    public TravelExtProfile fromVO(TravelExtProfileVO vo) {
        if (vo == null) return null;
        this.setId(vo.getId());
        this.setCognitoSub(vo.getCognitoSub());
        this.setTravelStyle(vo.getTravelStyle());
        this.setPreferredDestinations(vo.getPreferredDestinations());
        this.setBucketListPlaces(vo.getBucketListPlaces());
        this.setTripsPerYear(vo.getTripsPerYear());
        this.setPreferredTripDuration(vo.getPreferredTripDuration());
        this.setHasTraveledAbroad(vo.getHasTraveledAbroad());
        this.setCountriesVisited(vo.getCountriesVisited());
        this.setDietaryNeeds(vo.getDietaryNeeds());
        this.setIsOkWithBudgetStays(vo.getIsOkWithBudgetStays());
        this.setIsOkWithCamping(vo.getIsOkWithCamping());
        this.setPreferredGroupSize(vo.getPreferredGroupSize());
        this.setUpcomingTrips(vo.getUpcomingTrips());
        this.setPastTripsHighlights(vo.getPastTripsHighlights());
        return this;
    }

    public TravelExtProfileVO toVO() {
        TravelExtProfileVO vo = new TravelExtProfileVO();
        vo.setId(this.getId());
        vo.setCognitoSub(this.getCognitoSub());
        vo.setTravelStyle(this.getTravelStyle());
        vo.setPreferredDestinations(this.getPreferredDestinations());
        vo.setBucketListPlaces(this.getBucketListPlaces());
        vo.setTripsPerYear(this.getTripsPerYear());
        vo.setPreferredTripDuration(this.getPreferredTripDuration());
        vo.setHasTraveledAbroad(this.getHasTraveledAbroad());
        vo.setCountriesVisited(this.getCountriesVisited());
        vo.setDietaryNeeds(this.getDietaryNeeds());
        vo.setIsOkWithBudgetStays(this.getIsOkWithBudgetStays());
        vo.setIsOkWithCamping(this.getIsOkWithCamping());
        vo.setPreferredGroupSize(this.getPreferredGroupSize());
        vo.setUpcomingTrips(this.getUpcomingTrips());
        vo.setPastTripsHighlights(this.getPastTripsHighlights());
        return vo;
    }
}
