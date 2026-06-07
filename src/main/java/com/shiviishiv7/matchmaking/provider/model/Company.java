package com.shiviishiv7.matchmaking.provider.model;



import jakarta.persistence.*;
import lombok.*;

import java.util.List;
import java.util.UUID;

/**
 * Company — represents an employer.
 * Users must select a company and verify their work email
 * matches the company's domain before accessing the platform.
 */
@Entity
@Table(name = "companies", indexes = {
        @Index(name = "idx_company_domain", columnList = "domain", unique = true),
        @Index(name = "idx_company_name",   columnList = "name")
})
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class Company extends BaseEntity {

    @Id
    @GeneratedValue(strategy = GenerationType.UUID)
    @Column(name = "id", updatable = false, nullable = false)
    private UUID id;

    @Column(name = "name", nullable = false, length = 150)
    private String name;                        // e.g. "Google"

    @Column(name = "domain", nullable = false, unique = true, length = 100)
    private String domain;                      // e.g. "google.com"

    @Column(name = "industry", length = 100)
    private String industry;                    // e.g. "Technology"

    @Column(name = "logo_url", length = 500)
    private String logoUrl;                     // shown in UI search results

    @Column(name = "is_active", nullable = false)
    @Builder.Default
    private Boolean isActive = true;            // admin can deactivate a company

//    @OneToMany(mappedBy = "company", fetch = FetchType.LAZY)
//    private List<User> users;
}
