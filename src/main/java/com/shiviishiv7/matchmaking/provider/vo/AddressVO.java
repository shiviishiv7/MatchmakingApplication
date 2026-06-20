package com.shiviishiv7.matchmaking.provider.vo;


import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import com.shiviishiv7.matchmaking.provider.model.Address;
import com.shiviishiv7.matchmaking.provider.model.User;
import lombok.Getter;
import lombok.Setter;
import lombok.ToString;


@Getter
@Setter
@JsonIgnoreProperties(ignoreUnknown = true)
@ToString
public class AddressVO {

    private Integer id;
    private String cognitoSub; // Used to cleanly map ownership over REST contracts
    private String streetAddress;
    private String city;
    private String state;
    private String country;
    private String zip;

    public boolean validate() {
        if (country == null || country.trim().isEmpty()) {
            throw new IllegalArgumentException("Country cannot be empty");
        }
        if (zip != null && !zip.trim().isEmpty() && !zip.matches("^\\d{5,10}$")) {
            throw new IllegalArgumentException("ZIP code layout format is invalid");
        }
        return true;
    }

}