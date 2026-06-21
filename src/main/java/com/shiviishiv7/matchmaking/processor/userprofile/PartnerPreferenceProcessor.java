package com.shiviishiv7.matchmaking.processor.userprofile;

import com.shiviishiv7.matchmaking.common.exception.MatchmakingException;
import com.shiviishiv7.matchmaking.provider.implementation.PartnerPreferenceRepository;
import com.shiviishiv7.matchmaking.provider.model.PartnerPreference;

import com.shiviishiv7.matchmaking.provider.vo.BaseVO;
import com.shiviishiv7.matchmaking.provider.vo.PartnerPreferenceVO;

import jakarta.transaction.Transactional;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;

import java.util.Optional;

import static com.shiviishiv7.matchmaking.common.constants.MatchmakingHttpStatus.*;

@Component
@Transactional
@Slf4j
public class PartnerPreferenceProcessor implements IPartnerPreferenceProcessor {

    @Autowired
    private PartnerPreferenceRepository partnerPreferenceRepository;

    @Override
    public BaseVO add(PartnerPreferenceVO vo) throws MatchmakingException {
        try {
            log.info("Validating inputs for partner preference creation. {}", vo.toString());
            vo.validate();
            log.info("PartnerPreferenceVO validation completed successfully.");

            log.trace("Checking for duplicate partner preference for userId: {}", vo.getCognitoSub());
            if (partnerPreferenceRepository.existsByCognitoSub(vo.getCognitoSub())) {
                log.error("ALERT_FOR_ERROR: partner preference already exists for userId: {}", vo.getCognitoSub());
                throw new MatchmakingException("partner preference already exists for this user", DUPLICATE_RECORD);
            }

            log.trace("Saving partner preference for userId: {}", vo.getCognitoSub());
            PartnerPreference profile = new PartnerPreference();
            profile.fromVO(vo);
            profile = partnerPreferenceRepository.save(profile);
            log.info("partner preference saved successfully for userId: {}", profile.getCognitoSub());

            return new BaseVO(SUCCESS, "partner preference created", "partner preference created", profile.toVO());
        } catch (MatchmakingException ex) {
            throw ex;
        } catch (Exception ex) {
            log.error("ALERT_FOR_ERROR: Error occurred while adding partner preference. Error: {}", ex.getMessage(), ex);
            throw new MatchmakingException("Error occurred while adding partner preference: " + ex.getMessage(), UNKNOWN_EXCEPTION);
        }
    }

    @Override
    public BaseVO update(PartnerPreferenceVO vo) throws MatchmakingException {
        try {
            log.info("Validating inputs for partner preference update.");
            if (vo.getId() == null) {
                throw new MatchmakingException("PartnerPreference ID cannot be null for update", VALIDATION_ERROR);
            }
            vo.validate();
            log.info("PartnerPreferenceVO validation completed successfully.");

            log.trace("Fetching existing partner preference for ID: {}", vo.getId());
            Optional<PartnerPreference> fromDB = partnerPreferenceRepository.findById(vo.getId());
            if (fromDB.isEmpty()) {
                log.error("ALERT_FOR_ERROR: partner preference not found for ID: {}", vo.getId());
                throw new MatchmakingException("partner preference does not exist", DATA_NOT_FOUND);
            }

            PartnerPreference profile = fromDB.get();
            log.trace("partner preference found for ID: {}. Applying updates.", vo.getId());

            profile.setAgeMin(vo.getAgeMin());
            profile.setAgeMax(vo.getAgeMax());
            profile.setHeightMinCm(vo.getHeightMinCm());
            profile.setHeightMaxCm(vo.getHeightMaxCm());
            profile.setMaritalStatusPref(vo.getMaritalStatusPref());
            profile.setPreferredCountries(vo.getPreferredCountries());
            profile.setPreferredStates(vo.getPreferredStates());
            profile.setOpenToRelocation(vo.getOpenToRelocation());
            profile.setReligionPref(vo.getReligionPref());
            profile.setCastePref(vo.getCastePref());
            profile.setMotherTonguePref(vo.getMotherTonguePref());
            profile.setDietaryPref(vo.getDietaryPref());
            profile.setEducationPref(vo.getEducationPref());
            profile.setEmploymentTypePref(vo.getEmploymentTypePref());
            profile.setIncomeMinInr(vo.getIncomeMinInr());
            profile.setIncomeMaxInr(vo.getIncomeMaxInr());
            profile.setSmokingPref(vo.getSmokingPref());
            profile.setDrinkingPref(vo.getDrinkingPref());
            profile.setBodyTypePref(vo.getBodyTypePref());
            profile.setFamilyTypePref(vo.getFamilyTypePref());
            profile.setFamilyValuesPref(vo.getFamilyValuesPref());
            profile.setManglikPref(vo.getManglikPref());
            profile.setHoroscopeMatchRequired(vo.getHoroscopeMatchRequired());
            profile.setAboutPartner(vo.getAboutPartner());

            profile = partnerPreferenceRepository.save(profile);
            log.info("partner preference updated successfully for ID: {}", profile.getId());

            return new BaseVO(SUCCESS, "partner preference updated", "partner preference updated", profile.toVO());
        } catch (MatchmakingException ex) {
            throw ex;
        } catch (Exception ex) {
            log.error("ALERT_FOR_ERROR: Error occurred while updating partner preference. Error: {}", ex.getMessage(), ex);
            throw new MatchmakingException("Error occurred while updating partner preference: " + ex.getMessage(), UNKNOWN_EXCEPTION);
        }
    }

    @Override
    public BaseVO getById(String id) throws MatchmakingException {
        try {
            log.info("Fetching partner preference for ID: {}", id);
            Optional<PartnerPreference> fromDB = partnerPreferenceRepository.findById(Integer.valueOf(id));
            if (fromDB.isEmpty()) {
                log.error("ALERT_FOR_ERROR: partner preference not found for ID: {}", id);
                throw new MatchmakingException("partner preference does not exist", DATA_NOT_FOUND);
            }
            log.info("partner preference found for ID: {}", id);
            return new BaseVO(SUCCESS, "partner preference fetched", "partner preference fetched", fromDB.get().toVO());
        } catch (MatchmakingException ex) {
            throw ex;
        } catch (Exception ex) {
            log.error("ALERT_FOR_ERROR: Error occurred while fetching partner preference. Error: {}", ex.getMessage(), ex);
            throw new MatchmakingException("Error occurred while fetching partner preference: " + ex.getMessage(), UNKNOWN_EXCEPTION);
        }
    }

    @Override
    public BaseVO getByUserId(String userId) throws MatchmakingException {
        try {
            log.info("Fetching partner preference for userId: {}", userId);
            Optional<PartnerPreference> fromDB = partnerPreferenceRepository.findByCognitoSub(userId);
            if (fromDB.isEmpty()) {
                log.error("ALERT_FOR_ERROR: partner preference not found for userId: {}", userId);
                throw new MatchmakingException("partner preference does not exist for this user", DATA_NOT_FOUND);
            }
            log.info("partner preference found for userId: {}", userId);
            return new BaseVO(SUCCESS, "partner preference fetched", "partner preference fetched", fromDB.get().toVO());
        } catch (MatchmakingException ex) {
            throw ex;
        } catch (Exception ex) {
            log.error("ALERT_FOR_ERROR: Error occurred while fetching partner preference by userId. Error: {}", ex.getMessage(), ex);
            throw new MatchmakingException("Error occurred while fetching partner preference: " + ex.getMessage(), UNKNOWN_EXCEPTION);
        }
    }

    @Override
    public BaseVO delete(String id) throws MatchmakingException {
        try {
            log.info("Deleting partner preference for ID: {}", id);
            Optional<PartnerPreference> fromDB = partnerPreferenceRepository.findById(Integer.valueOf(id));
            if (fromDB.isEmpty()) {
                log.error("ALERT_FOR_ERROR: partner preference not found for ID: {}", id);
                throw new MatchmakingException("partner preference does not exist", DATA_NOT_FOUND);
            }
            partnerPreferenceRepository.deleteById(Integer.valueOf(id));
            log.info("partner preference hard-deleted for ID: {}", id);
            return new BaseVO(SUCCESS, "partner preference deleted", "partner preference deleted");
        } catch (MatchmakingException ex) {
            throw ex;
        } catch (Exception ex) {
            log.error("ALERT_FOR_ERROR: Error occurred while deleting partner preference. Error: {}", ex.getMessage(), ex);
            throw new MatchmakingException("Error occurred while deleting partner preference: " + ex.getMessage(), UNKNOWN_EXCEPTION);
        }
    }
}
