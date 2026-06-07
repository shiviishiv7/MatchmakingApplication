package com.shiviishiv7.matchmaking.provider.vo;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import com.shiviishiv7.matchmaking.provider.model.Company;
import lombok.Getter;
import lombok.Setter;

import java.util.UUID;

@Getter
@Setter
@JsonIgnoreProperties(ignoreUnknown = true)
public class CompanyVO {

    private UUID id;
    private String name;
    private String domain;
    private String industry;
    private String logoUrl;
    private Boolean isActive;

    public boolean validate() {
        if (name == null || name.isEmpty()) {
            throw new IllegalArgumentException("Name cannot be empty");
        }
        if (domain == null || domain.isEmpty()) {
            throw new IllegalArgumentException("Domain cannot be empty");
        }
        return true;
    }

    public Company fromVO() {
        Company company = new Company();
        company.setId(id);
        company.setName(name);
        company.setDomain(domain);
        company.setIndustry(industry);
        company.setLogoUrl(logoUrl);
        if (isActive != null) {
            company.setIsActive(isActive);
        }
        return company;
    }

    public CompanyVO toVO(Company company) {
        CompanyVO vo = new CompanyVO();
        vo.setId(company.getId());
        vo.setName(company.getName());
        vo.setDomain(company.getDomain());
        vo.setIndustry(company.getIndustry());
        vo.setLogoUrl(company.getLogoUrl());
        vo.setIsActive(company.getIsActive());
        return vo;
    }
}
