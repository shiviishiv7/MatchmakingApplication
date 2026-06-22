package com.shiviishiv7.matchmaking.provider.model;

import com.shiviishiv7.matchmaking.provider.vo.AddressVO;
import jakarta.persistence.*;
import lombok.*;


@Entity
@Table(name = "userAddress")
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class Address extends BaseEntity {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    @Column(name = "id", updatable = false, nullable = false)
    private Integer id;

    @Column(name = "cognitoSub", nullable = false)
    private String cognitoSub;

    @Column(name = "streetAddress", length = 500)
    private String streetAddress;

    @Column(name = "city", length = 100)
    private String city;

    @Column(name = "state", length = 100)
    private String state;

    @Column(name = "country", nullable = false, length = 100)
    private String country;

    @Column(name = "zip", length = 20)
    private String zip;

    public Address fromVO(AddressVO vo) {
        if (vo == null) {
            return null;
        }
        this.setId(vo.getId());
        this.setCognitoSub(vo.getCognitoSub());
        this.setStreetAddress(vo.getStreetAddress());
        this.setCity(vo.getCity());
        this.setState(vo.getState());
        this.setCountry(vo.getCountry());
        this.setZip(vo.getZip());
        return this;
    }

    /**
     * Maps this Address entity instance state out into a clean AddressVO contract layout.
     * @return A newly built value object configuration tracking state values
     */
    public AddressVO toVO() {
        AddressVO vo = new AddressVO();
        vo.setId(this.getId());
        vo.setCognitoSub(this.getCognitoSub());
        vo.setStreetAddress(this.getStreetAddress());
        vo.setCity(this.getCity());
        vo.setState(this.getState());
        vo.setCountry(this.getCountry());
        vo.setZip(this.getZip());
        return vo;
    }
}
