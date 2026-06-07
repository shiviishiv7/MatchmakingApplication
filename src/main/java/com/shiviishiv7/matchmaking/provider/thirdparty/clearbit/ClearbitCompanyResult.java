package com.shiviishiv7.matchmaking.provider.thirdparty.clearbit;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import lombok.Getter;
import lombok.Setter;

/**
 * Maps one entry from the Clearbit Autocomplete API response:
 * GET https://autocomplete.clearbit.com/v1/companies/suggest?query={name}
 */
@Getter
@Setter
@JsonIgnoreProperties(ignoreUnknown = true)
public class ClearbitCompanyResult {

    private String name;    // e.g. "Google"
    private String domain;  // e.g. "google.com"
    private String logo;    // e.g. "https://logo.clearbit.com/google.com"
}
