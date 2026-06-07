package com.shiviishiv7.matchmaking.provider.thirdparty.clearbit;

import com.shiviishiv7.matchmaking.common.exception.MatchmakingException;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.core.ParameterizedTypeReference;
import org.springframework.stereotype.Component;
import org.springframework.web.client.RestClient;

import java.util.Collections;
import java.util.List;

import static com.shiviishiv7.matchmaking.common.constants.MatchmakingHttpStatus.UNKNOWN_EXCEPTION;

@Component
@Slf4j
public class ClearbitApiClient {

    private final RestClient restClient;

    @Value("${thirdparty.clearbit.autocomplete-url}")
    private String autocompleteUrl;

    public ClearbitApiClient() {
        this.restClient = RestClient.builder()
                .defaultHeader("Accept", "application/json")
                .build();
    }

    /**
     * Calls the Clearbit Autocomplete API and returns up to 5 matching companies.
     * No API key required — this endpoint is publicly available.
     */
    public List<ClearbitCompanyResult> searchCompanies(String query) throws MatchmakingException {
        try {
            log.info("Calling Clearbit Autocomplete API for query: {}", query);

            List<ClearbitCompanyResult> results = restClient.get()
                    .uri(autocompleteUrl + "?query={query}", query)
                    .retrieve()
                    .body(new ParameterizedTypeReference<List<ClearbitCompanyResult>>() {});

            if (results == null || results.isEmpty()) {
                log.info("Clearbit returned no results for query: {}", query);
                return Collections.emptyList();
            }

            log.info("Clearbit returned {} result(s) for query: {}", results.size(), query);
            return results;

        } catch (MatchmakingException ex) {
            throw ex;
        } catch (Exception ex) {
            log.error("ALERT_FOR_ERROR: Failed to call Clearbit API for query: {}. Error: {}", query, ex.getMessage(), ex);
            throw new MatchmakingException("Failed to fetch company data from external source: " + ex.getMessage(), UNKNOWN_EXCEPTION);
        }
    }
}
