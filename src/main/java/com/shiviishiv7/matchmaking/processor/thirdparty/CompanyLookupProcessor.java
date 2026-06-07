package com.shiviishiv7.matchmaking.processor.thirdparty;

import com.shiviishiv7.matchmaking.common.exception.MatchmakingException;
import com.shiviishiv7.matchmaking.provider.implementation.CompanyRepository;
import com.shiviishiv7.matchmaking.provider.model.Company;
import com.shiviishiv7.matchmaking.provider.thirdparty.clearbit.ClearbitApiClient;
import com.shiviishiv7.matchmaking.provider.thirdparty.clearbit.ClearbitCompanyResult;
import com.shiviishiv7.matchmaking.provider.vo.BaseVO;
import com.shiviishiv7.matchmaking.provider.vo.CompanyVO;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;

import java.util.ArrayList;
import java.util.List;
import java.util.stream.Collectors;

import static com.shiviishiv7.matchmaking.common.constants.MatchmakingHttpStatus.*;

@Component
@Slf4j
public class CompanyLookupProcessor implements ICompanyLookupProcessor {

    @Autowired
    private CompanyRepository companyRepository;

    @Autowired
    private ClearbitApiClient clearbitApiClient;

    @Override
    public BaseVO searchForSignup(String name) throws MatchmakingException {
        try {
            log.info("Company signup search started for name: {}", name);

            if (name == null || name.isBlank()) {
                throw new MatchmakingException("Search term cannot be empty", VALIDATION_ERROR);
            }

            // Step 1 — check local DB first
            log.trace("Searching local DB for company name: {}", name);
            List<Company> localResults = companyRepository.searchByName(name);

            if (!localResults.isEmpty()) {
                log.info("Found {} local result(s) for name: {}. Returning from DB.", localResults.size(), name);
                List<CompanyVO> result = localResults.stream()
                        .map(c -> new CompanyVO().toVO(c))
                        .collect(Collectors.toList());
                return new BaseVO(SUCCESS, "Companies found", "Results from local database", result);
            }

            // Step 2 — nothing in local DB, call Clearbit
            log.info("No local results for name: {}. Falling back to Clearbit API.", name);
            List<ClearbitCompanyResult> clearbitResults = clearbitApiClient.searchCompanies(name);

            if (clearbitResults.isEmpty()) {
                log.error("ALERT_FOR_ERROR: No results found for company name: {} in DB or Clearbit.", name);
                throw new MatchmakingException("No companies found for: " + name, DATA_NOT_FOUND);
            }

            // Map Clearbit results to CompanyVO (not persisted yet — user must confirm selection)
            List<CompanyVO> result = mapClearbitToVO(clearbitResults);
            log.info("Clearbit returned {} result(s) for name: {}.", result.size(), name);

            return new BaseVO(SUCCESS, "Companies found", "Results from external source (Clearbit)", result);

        } catch (MatchmakingException ex) {
            throw ex;
        } catch (Exception ex) {
            log.error("ALERT_FOR_ERROR: Error occurred during company signup search for name: {}. Error: {}", name, ex.getMessage(), ex);
            throw new MatchmakingException("Error occurred while searching for company: " + ex.getMessage(), UNKNOWN_EXCEPTION);
        }
    }

    private List<CompanyVO> mapClearbitToVO(List<ClearbitCompanyResult> clearbitResults) {
        List<CompanyVO> vos = new ArrayList<>();
        for (ClearbitCompanyResult result : clearbitResults) {
            CompanyVO vo = new CompanyVO();
            vo.setName(result.getName());
            vo.setDomain(result.getDomain());
            vo.setLogoUrl(result.getLogo());
            vo.setIsActive(true);
            vos.add(vo);
        }
        return vos;
    }
}
