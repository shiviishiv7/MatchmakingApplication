package com.shiviishiv7.matchmaking.processor.company;

import com.shiviishiv7.matchmaking.common.exception.MatchmakingException;
import com.shiviishiv7.matchmaking.provider.implementation.CompanyRepository;
import com.shiviishiv7.matchmaking.provider.model.Company;
import com.shiviishiv7.matchmaking.provider.vo.BaseVO;
import com.shiviishiv7.matchmaking.provider.vo.CompanyVO;
import jakarta.transaction.Transactional;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;

import java.util.List;
import java.util.Optional;

import java.util.stream.Collectors;

import static com.shiviishiv7.matchmaking.common.constants.MatchmakingHttpStatus.*;

@Component
@Transactional
@Slf4j
public class CompanyProcessor implements ICompanyProcessor {

    @Autowired
    private CompanyRepository companyRepository;

    @Override
    public BaseVO add(CompanyVO companyVO) throws MatchmakingException {
        try {
            log.info("Validating inputs for company creation.");
            companyVO.validate();
            log.info("CompanyVO validation completed successfully.");

            log.trace("Checking for duplicate domain: {}", companyVO.getDomain());
            if (companyRepository.existsByDomain(companyVO.getDomain())) {
                log.error("ALERT_FOR_ERROR: Duplicate domain found: {}", companyVO.getDomain());
                throw new MatchmakingException("Company with domain already exists", DUPLICATE_RECORD);
            }

            log.trace("Saving company record for domain: {}", companyVO.getDomain());
            Company company = companyVO.fromVO();
            company = companyRepository.save(company);
            log.info("Company record saved successfully for domain: {}", company.getDomain());

            return new BaseVO(SUCCESS, "Company record saved", "Company record saved", new CompanyVO().toVO(company));
        } catch (MatchmakingException ex) {
            throw ex;
        } catch (Exception ex) {
            log.error("ALERT_FOR_ERROR: Error occurred while adding company. Error: {}", ex.getMessage(), ex);
            throw new MatchmakingException("Error occurred while adding company: " + ex.getMessage(), UNKNOWN_EXCEPTION);
        }
    }

    @Override
    public BaseVO update(CompanyVO companyVO) throws MatchmakingException {
        try {
            log.info("Validating inputs for company update.");
            if (companyVO.getId() == null) {
                throw new MatchmakingException("Company ID cannot be null for update", VALIDATION_ERROR);
            }
            companyVO.validate();
            log.info("CompanyVO validation completed successfully.");

            log.trace("Fetching existing company for ID: {}", companyVO.getId());
            Optional<Company> companyFromDBOptional = companyRepository.findById(companyVO.getId());
            if (companyFromDBOptional.isEmpty()) {
                log.error("ALERT_FOR_ERROR: Company not found for ID: {}", companyVO.getId());
                throw new MatchmakingException("Company does not exist", DATA_NOT_FOUND);
            }

            Company companyFromDB = companyFromDBOptional.get();
            log.trace("Company found for ID: {}. Applying updates.", companyVO.getId());

            companyFromDB.setName(companyVO.getName());
            companyFromDB.setDomain(companyVO.getDomain());
            companyFromDB.setIndustry(companyVO.getIndustry());
            companyFromDB.setLogoUrl(companyVO.getLogoUrl());
            if (companyVO.getIsActive() != null) {
                companyFromDB.setIsActive(companyVO.getIsActive());
            }

            companyFromDB = companyRepository.save(companyFromDB);
            log.info("Company record updated successfully for ID: {}", companyFromDB.getId());

            return new BaseVO(SUCCESS, "Company record updated", "Company record updated", new CompanyVO().toVO(companyFromDB));
        } catch (MatchmakingException ex) {
            throw ex;
        } catch (Exception ex) {
            log.error("ALERT_FOR_ERROR: Error occurred while updating company. Error: {}", ex.getMessage(), ex);
            throw new MatchmakingException("Error occurred while updating company: " + ex.getMessage(), UNKNOWN_EXCEPTION);
        }
    }

    @Override
    public BaseVO get(String id) throws MatchmakingException {
        try {
            log.info("Fetching company for ID: {}", id);
            Optional<Company> optionalCompany = companyRepository.findById(Integer.valueOf(id));
            if (optionalCompany.isEmpty()) {
                log.error("ALERT_FOR_ERROR: Company not found for ID: {}", id);
                throw new MatchmakingException("Company does not exist", DATA_NOT_FOUND);
            }

            log.info("Company found for ID: {}", id);
            return new BaseVO(SUCCESS, "Company record fetched", "Company record fetched", new CompanyVO().toVO(optionalCompany.get()));
        } catch (MatchmakingException ex) {
            throw ex;
        } catch (Exception ex) {
            log.error("ALERT_FOR_ERROR: Error occurred while fetching company. Error: {}", ex.getMessage(), ex);
            throw new MatchmakingException("Error occurred while fetching company: " + ex.getMessage(), UNKNOWN_EXCEPTION);
        }
    }

    @Override
    public BaseVO getAll() throws MatchmakingException {
        try {
            log.info("Fetching all companies.");
            List<Company> companies = companyRepository.findAll();
            if (companies.isEmpty()) {
                log.error("ALERT_FOR_ERROR: No companies found.");
                throw new MatchmakingException("No companies found", DATA_NOT_FOUND);
            }

            List<CompanyVO> result = companies.stream()
                    .map(c -> new CompanyVO().toVO(c))
                    .collect(Collectors.toList());

            log.info("Fetched {} companies.", result.size());
            return new BaseVO(SUCCESS, "Companies fetched", "All company records fetched", result);
        } catch (MatchmakingException ex) {
            throw ex;
        } catch (Exception ex) {
            log.error("ALERT_FOR_ERROR: Error occurred while fetching companies. Error: {}", ex.getMessage(), ex);
            throw new MatchmakingException("Error occurred while fetching companies: " + ex.getMessage(), UNKNOWN_EXCEPTION);
        }
    }

    @Override
    public BaseVO search(String name) throws MatchmakingException {
        try {
            log.info("Searching companies by name: {}", name);
            if (name == null || name.isBlank()) {
                throw new MatchmakingException("Search term cannot be empty", VALIDATION_ERROR);
            }

            List<Company> companies = companyRepository.searchByName(name);
            if (companies.isEmpty()) {
                log.error("ALERT_FOR_ERROR: No companies found for search term: {}", name);
                throw new MatchmakingException("No companies found for the given search term", DATA_NOT_FOUND);
            }

            List<CompanyVO> result = companies.stream()
                    .map(c -> new CompanyVO().toVO(c))
                    .collect(Collectors.toList());

            log.info("Found {} companies for search term: {}", result.size(), name);
            return new BaseVO(SUCCESS, "Companies fetched", "Company search results", result);
        } catch (MatchmakingException ex) {
            throw ex;
        } catch (Exception ex) {
            log.error("ALERT_FOR_ERROR: Error occurred while searching companies. Error: {}", ex.getMessage(), ex);
            throw new MatchmakingException("Error occurred while searching companies: " + ex.getMessage(), UNKNOWN_EXCEPTION);
        }
    }

    @Override
    public BaseVO delete(String id) throws MatchmakingException {
        try {
            log.info("Deleting company for ID: {}", id);
            Optional<Company> optionalCompany = companyRepository.findById(Integer.valueOf(id));
            if (optionalCompany.isEmpty()) {
                log.error("ALERT_FOR_ERROR: Company not found for ID: {}", id);
                throw new MatchmakingException("Company does not exist", DATA_NOT_FOUND);
            }

            Company company = optionalCompany.get();
            company.setIsActive(false);
            companyRepository.save(company);
            log.info("Company soft-deleted (deactivated) for ID: {}", id);

            return new BaseVO(SUCCESS, "Company deactivated", "Company record deactivated");
        } catch (MatchmakingException ex) {
            throw ex;
        } catch (Exception ex) {
            log.error("ALERT_FOR_ERROR: Error occurred while deleting company. Error: {}", ex.getMessage(), ex);
            throw new MatchmakingException("Error occurred while deleting company: " + ex.getMessage(), UNKNOWN_EXCEPTION);
        }
    }
}
