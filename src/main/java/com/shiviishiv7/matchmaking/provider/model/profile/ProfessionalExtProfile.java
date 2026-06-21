package com.shiviishiv7.matchmaking.provider.model.profile;

import com.shiviishiv7.matchmaking.provider.model.BaseEntity;
import com.shiviishiv7.matchmaking.provider.vo.ProfessionalExtProfileVO;
import jakarta.persistence.*;
import lombok.*;

@Entity
@Table(name = "extProfessionalProfiles")
@Getter @Setter @NoArgsConstructor @AllArgsConstructor @Builder
public class ProfessionalExtProfile extends BaseEntity {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    @Column(name = "id", updatable = false, nullable = false)
    private Integer id;

    @Column(name = "userId", nullable = false, unique = true)
    private String cognitoSub;

    @Column(name = "currentRole", length = 100)
    private String currentRole;

    @Column(name = "currentCompany", length = 150)
    private String currentCompany;

    @Column(name = "yearsOfExperience")
    private Integer yearsOfExperience;

    @Column(name = "industryDomain", length = 100)
    private String industryDomain;

    @Column(name = "techStack", length = 500)
    private String techStack;

    @Column(name = "skillsOffering", length = 500)
    private String skillsOffering;

    @Column(name = "skillsSeeking", length = 500)
    private String skillsSeeking;

    @Column(name = "mentorshipRole", length = 20)
    private String mentorshipRole;

    @Column(name = "openToCoFounder")
    @Builder.Default
    private Boolean openToCoFounder = false;

    @Column(name = "startupIdeas", columnDefinition = "TEXT")
    private String startupIdeas;

    @Column(name = "linkedinUrl", length = 300)
    private String linkedinUrl;

    @Column(name = "githubUrl", length = 300)
    private String githubUrl;

    @Column(name = "portfolioUrl", length = 300)
    private String portfolioUrl;

    @Column(name = "certifications", length = 500)
    private String certifications;

    @Column(name = "preferredCollabMode", length = 30)
    private String preferredCollabMode;

    @Column(name = "availabilitySlots", columnDefinition = "JSON")
    private String availabilitySlots;

    public ProfessionalExtProfile fromVO(ProfessionalExtProfileVO vo) {
        if (vo == null) return null;
        this.setId(vo.getId());
        this.setCognitoSub(vo.getCognitoSub());
        this.setCurrentRole(vo.getCurrentRole());
        this.setCurrentCompany(vo.getCurrentCompany());
        this.setYearsOfExperience(vo.getYearsOfExperience());
        this.setIndustryDomain(vo.getIndustryDomain());
        this.setTechStack(vo.getTechStack());
        this.setSkillsOffering(vo.getSkillsOffering());
        this.setSkillsSeeking(vo.getSkillsSeeking());
        this.setMentorshipRole(vo.getMentorshipRole());
        this.setOpenToCoFounder(vo.getOpenToCoFounder());
        this.setStartupIdeas(vo.getStartupIdeas());
        this.setLinkedinUrl(vo.getLinkedinUrl());
        this.setGithubUrl(vo.getGithubUrl());
        this.setPortfolioUrl(vo.getPortfolioUrl());
        this.setCertifications(vo.getCertifications());
        this.setPreferredCollabMode(vo.getPreferredCollabMode());
        this.setAvailabilitySlots(vo.getAvailabilitySlots());
        return this;
    }

    public ProfessionalExtProfileVO toVO() {
        ProfessionalExtProfileVO vo = new ProfessionalExtProfileVO();
        vo.setId(this.getId());
        vo.setCognitoSub(this.getCognitoSub());
        vo.setCurrentRole(this.getCurrentRole());
        vo.setCurrentCompany(this.getCurrentCompany());
        vo.setYearsOfExperience(this.getYearsOfExperience());
        vo.setIndustryDomain(this.getIndustryDomain());
        vo.setTechStack(this.getTechStack());
        vo.setSkillsOffering(this.getSkillsOffering());
        vo.setSkillsSeeking(this.getSkillsSeeking());
        vo.setMentorshipRole(this.getMentorshipRole());
        vo.setOpenToCoFounder(this.getOpenToCoFounder());
        vo.setStartupIdeas(this.getStartupIdeas());
        vo.setLinkedinUrl(this.getLinkedinUrl());
        vo.setGithubUrl(this.getGithubUrl());
        vo.setPortfolioUrl(this.getPortfolioUrl());
        vo.setCertifications(this.getCertifications());
        vo.setPreferredCollabMode(this.getPreferredCollabMode());
        vo.setAvailabilitySlots(this.getAvailabilitySlots());
        return vo;
    }
}
