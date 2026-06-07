package com.shiviishiv7.matchmaking.provider.implementation;


import com.shiviishiv7.matchmaking.provider.model.Company;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;
import java.util.UUID;

@Repository
public interface CompanyRepository extends JpaRepository<Company, UUID> {

    Optional<Company> findByDomain(String domain);

    boolean existsByDomain(String domain);

    // Typeahead search — case-insensitive, active companies only
    @Query("SELECT c FROM Company c WHERE LOWER(c.name) LIKE LOWER(CONCAT('%', :name, '%')) AND c.isActive = true ORDER BY c.name")
    List<Company> searchByName(String name);
}
