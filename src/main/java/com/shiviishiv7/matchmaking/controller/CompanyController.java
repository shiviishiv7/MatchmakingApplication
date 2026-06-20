package com.shiviishiv7.matchmaking.controller;

import com.shiviishiv7.matchmaking.common.exception.MatchmakingException;
import com.shiviishiv7.matchmaking.common.security.MatchmakingSecurityUtility;
import com.shiviishiv7.matchmaking.processor.company.ICompanyProcessor;
import com.shiviishiv7.matchmaking.processor.thirdparty.ICompanyLookupProcessor;
import com.shiviishiv7.matchmaking.provider.vo.BaseVO;
import com.shiviishiv7.matchmaking.provider.vo.CompanyVO;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;



@RestController
@RequestMapping("/company")
@Slf4j
@Tag(name = "Company", description = "Company search and management")
public class CompanyController {

    @Autowired
    private ICompanyProcessor companyProcessor;

    @Autowired
    private ICompanyLookupProcessor companyLookupProcessor;

    @Autowired
    private MatchmakingSecurityUtility securityUtility;

    @RequestMapping(value = "/add", method = RequestMethod.POST, produces = "application/json", consumes = "application/json")
    @ResponseBody
    public ResponseEntity<BaseVO> add(@RequestBody CompanyVO companyVO) throws MatchmakingException {
        String sub = securityUtility.getAuthenticatedUserSub();
        log.info("Request received to add company with name: {} by user: {}", companyVO.getName(), sub);

        BaseVO response = companyProcessor.add(companyVO);
        log.info("Successfully added company with name: {} by user: {}", companyVO.getName(), sub);
        return new ResponseEntity<>(response, HttpStatus.OK);
    }

    @RequestMapping(value = "/update", method = RequestMethod.POST, produces = "application/json", consumes = "application/json")
    @ResponseBody
    public ResponseEntity<BaseVO> update(@RequestBody CompanyVO companyVO) throws MatchmakingException {
        String sub = securityUtility.getAuthenticatedUserSub();
        log.info("Request received to update company with ID: {} by user: {}", companyVO.getId(), sub);

        BaseVO response = companyProcessor.update(companyVO);
        log.info("Successfully updated company with ID: {} by user: {}", companyVO.getId(), sub);
        return new ResponseEntity<>(response, HttpStatus.OK);
    }

    @RequestMapping(value = "/{id}", method = RequestMethod.GET, produces = "application/json")
    @ResponseBody
    public ResponseEntity<BaseVO> get(@PathVariable("id") String id) throws MatchmakingException {
        String sub = securityUtility.getAuthenticatedUserSub();
        log.info("Request received to fetch company for ID: {} by user: {}", id, sub);

        BaseVO response = companyProcessor.get(id);
        log.info("Successfully fetched company for ID: {} by user: {}", id, sub);
        return new ResponseEntity<>(response, HttpStatus.OK);
    }

    @RequestMapping(value = "/all", method = RequestMethod.GET, produces = "application/json")
    @ResponseBody
    public ResponseEntity<BaseVO> getAll() throws MatchmakingException {
        String sub = securityUtility.getAuthenticatedUserSub();
        log.info("Request received to fetch all companies by user: {}", sub);

        BaseVO response = companyProcessor.getAll();
        log.info("Successfully fetched all companies by user: {}", sub);
        return new ResponseEntity<>(response, HttpStatus.OK);
    }

    @RequestMapping(value = "/search", method = RequestMethod.GET, produces = "application/json")
    @ResponseBody
    public ResponseEntity<BaseVO> search(@RequestParam(required = true) String name) throws MatchmakingException {
        String sub = securityUtility.getAuthenticatedUserSub();
        log.info("Request received to search companies with name: {} by user: {}", name, sub);

        BaseVO response = companyProcessor.search(name);
        log.info("Successfully searched companies with name: {} by user: {}", name, sub);
        return new ResponseEntity<>(response, HttpStatus.OK);
    }

    /**
     * Signup typeahead search — checks local DB first, falls back to Clearbit if nothing found.
     * Does NOT persist results. When user confirms their company, call POST /company/add.
     */
    @RequestMapping(value = "/search/signup", method = RequestMethod.GET, produces = "application/json")
    @ResponseBody
    public ResponseEntity<BaseVO> searchForSignup(@RequestParam(required = true) String name) throws MatchmakingException {
        log.info("Signup company search request received for name: {}", name);

        BaseVO response = companyLookupProcessor.searchForSignup(name);
        log.info("Successfully completed signup company search for name: {}", name);
        return new ResponseEntity<>(response, HttpStatus.OK);
    }

    @RequestMapping(value = "/delete/{id}", method = RequestMethod.POST, produces = "application/json")
    @ResponseBody
    public ResponseEntity<BaseVO> delete(@PathVariable("id") String id) throws MatchmakingException {
        String sub = securityUtility.getAuthenticatedUserSub();
        log.info("Request received to delete company with ID: {} by user: {}", id, sub);

        BaseVO response = companyProcessor.delete(id);
        log.info("Successfully deleted company with ID: {} by user: {}", id, sub);
        return new ResponseEntity<>(response, HttpStatus.OK);
    }
}
