package com.shiviishiv7.matchmaking.processor.userprofile;

import com.shiviishiv7.matchmaking.common.exception.MatchmakingException;
import com.shiviishiv7.matchmaking.provider.implementation.FlatmateExtProfileRepository;
import com.shiviishiv7.matchmaking.provider.model.profile.FlatmateExtProfile;

import com.shiviishiv7.matchmaking.provider.vo.BaseVO;
import com.shiviishiv7.matchmaking.provider.vo.MatchFilterVO;
import com.shiviishiv7.matchmaking.provider.vo.FlatmateExtProfileVO;

import jakarta.transaction.Transactional;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;

import java.util.Optional;

import static com.shiviishiv7.matchmaking.common.constants.MatchmakingHttpStatus.*;

@Component
@Transactional
@Slf4j
public class FlatmateExtProfileProcessor implements IFlatmateExtProfileProcessor {

    @Autowired
    private FlatmateExtProfileRepository flatmateExtProfileRepository;

    @Override
    public BaseVO add(FlatmateExtProfileVO vo) throws MatchmakingException {
        try {
            log.info("Validating inputs for flatmate profile creation. {}", vo.toString());
            vo.validate();
            log.info("FlatmateExtProfileVO validation completed successfully.");

            log.trace("Checking for duplicate flatmate profile for userId: {}", vo.getCognitoSub());
            if (flatmateExtProfileRepository.existsByCognitoSub(vo.getCognitoSub())) {
                log.error("ALERT_FOR_ERROR: flatmate profile already exists for userId: {}", vo.getCognitoSub());
                throw new MatchmakingException("flatmate profile already exists for this user", DUPLICATE_RECORD);
            }

            log.trace("Saving flatmate profile for userId: {}", vo.getCognitoSub());
            FlatmateExtProfile profile = new FlatmateExtProfile();
            profile.fromVO(vo);
            profile = flatmateExtProfileRepository.save(profile);
            log.info("flatmate profile saved successfully for userId: {}", profile.getCognitoSub());

            return new BaseVO(SUCCESS, "flatmate profile created", "flatmate profile created", profile.toVO());
        } catch (MatchmakingException ex) {
            throw ex;
        } catch (Exception ex) {
            log.error("ALERT_FOR_ERROR: Error occurred while adding flatmate profile. Error: {}", ex.getMessage(), ex);
            throw new MatchmakingException("Error occurred while adding flatmate profile: " + ex.getMessage(), UNKNOWN_EXCEPTION);
        }
    }

    @Override
    public BaseVO update(FlatmateExtProfileVO vo) throws MatchmakingException {
        try {
            log.info("Validating inputs for flatmate profile update.");
            if (vo.getCognitoSub() == null) {
                throw new MatchmakingException("cognitoSub cannot be null for update", VALIDATION_ERROR);
            }
            vo.validate();
            log.info("FlatmateExtProfileVO validation completed successfully.");

            log.trace("Fetching profile for cognitoSub: {}", vo.getId());
            Optional<FlatmateExtProfile> fromDB = flatmateExtProfileRepository.findByCognitoSub(vo.getCognitoSub());
            if (fromDB.isEmpty()) {
                log.error("ALERT_FOR_ERROR: flatmate profile not found for cognitoSub: {}", vo.getId());
                throw new MatchmakingException("flatmate profile does not exist", DATA_NOT_FOUND);
            }

            FlatmateExtProfile profile = fromDB.get();
            log.trace("flatmate profile found for cognitoSub: {}. Applying updates.", vo.getId());

            profile.setLookingIn(vo.getLookingIn());
            profile.setBudgetRangeInr(vo.getBudgetRangeInr());
            profile.setMoveInDate(vo.getMoveInDate());
            profile.setPreferredFlatmateGender(vo.getPreferredFlatmateGender());
            profile.setOccupationType(vo.getOccupationType());
            profile.setIsVegetarianHousehold(vo.getIsVegetarianHousehold());
            profile.setAllowsSmoking(vo.getAllowsSmoking());
            profile.setHasPets(vo.getHasPets());
            profile.setAllowsPets(vo.getAllowsPets());
            profile.setSleepSchedule(vo.getSleepSchedule());
            profile.setCleanlinessLevel(vo.getCleanlinessLevel());
            profile.setGuestsPolicy(vo.getGuestsPolicy());
            profile.setHasCurrentFlat(vo.getHasCurrentFlat());
            profile.setCurrentFlatDetails(vo.getCurrentFlatDetails());

            profile = flatmateExtProfileRepository.save(profile);
            log.info("flatmate profile updated successfully for ID: {}", profile.getId());

            return new BaseVO(SUCCESS, "flatmate profile updated", "flatmate profile updated", profile.toVO());
        } catch (MatchmakingException ex) {
            throw ex;
        } catch (Exception ex) {
            log.error("ALERT_FOR_ERROR: Error occurred while updating flatmate profile. Error: {}", ex.getMessage(), ex);
            throw new MatchmakingException("Error occurred while updating flatmate profile: " + ex.getMessage(), UNKNOWN_EXCEPTION);
        }
    }

    @Override
    public BaseVO getByCognitoSub(String cognitoSub) throws MatchmakingException {
        try {
            log.info("Fetching profile for cognitoSub: {}", cognitoSub);
            Optional<FlatmateExtProfile> fromDB = flatmateExtProfileRepository.findByCognitoSub(cognitoSub);
            if (fromDB.isEmpty()) {
                log.error("ALERT_FOR_ERROR: profile not found for cognitoSub: {}", cognitoSub);
                throw new MatchmakingException("flatmate profile does not exist", DATA_NOT_FOUND);
            }
            log.info("Profile found for cognitoSub: {}", cognitoSub);
            return new BaseVO(SUCCESS, "flatmate profile fetched", "flatmate profile fetched", fromDB.get().toVO());
        } catch (MatchmakingException ex) {
            throw ex;
        } catch (Exception ex) {
            log.error("ALERT_FOR_ERROR: Error occurred while fetching flatmate profile. Error: {}", ex.getMessage(), ex);
            throw new MatchmakingException("Error occurred while fetching flatmate profile: " + ex.getMessage(), UNKNOWN_EXCEPTION);
        }
    }

    @Override
    public BaseVO getByUserId(String userId) throws MatchmakingException {
        try {
            log.info("Fetching flatmate profile for userId: {}", userId);
            Optional<FlatmateExtProfile> fromDB = flatmateExtProfileRepository.findByCognitoSub(userId);
            if (fromDB.isEmpty()) {
                log.error("ALERT_FOR_ERROR: flatmate profile not found for userId: {}", userId);
                throw new MatchmakingException("flatmate profile does not exist for this user", DATA_NOT_FOUND);
            }
            log.info("flatmate profile found for userId: {}", userId);
            return new BaseVO(SUCCESS, "flatmate profile fetched", "flatmate profile fetched", fromDB.get().toVO());
        } catch (MatchmakingException ex) {
            throw ex;
        } catch (Exception ex) {
            log.error("ALERT_FOR_ERROR: Error occurred while fetching flatmate profile by userId. Error: {}", ex.getMessage(), ex);
            throw new MatchmakingException("Error occurred while fetching flatmate profile: " + ex.getMessage(), UNKNOWN_EXCEPTION);
        }
    }

    @Override
    public BaseVO delete(String cognitoSub) throws MatchmakingException {
        try {
            log.info("Deleting flatmate profile for cognitoSub: {}", cognitoSub);
            Optional<FlatmateExtProfile> fromDB = flatmateExtProfileRepository.findByCognitoSub(cognitoSub);
            if (fromDB.isEmpty()) {
                log.error("ALERT_FOR_ERROR: profile not found for cognitoSub: {}", cognitoSub);
                throw new MatchmakingException("flatmate profile does not exist", DATA_NOT_FOUND);
            }
            flatmateExtProfileRepository.delete(fromDB.get());
            log.info("flatmate profile hard-deleted for cognitoSub: {}", cognitoSub);
            return new BaseVO(SUCCESS, "flatmate profile deleted", "flatmate profile deleted");
        } catch (MatchmakingException ex) {
            throw ex;
        } catch (Exception ex) {
            log.error("ALERT_FOR_ERROR: Error occurred while deleting flatmate profile. Error: {}", ex.getMessage(), ex);
            throw new MatchmakingException("Error occurred while deleting flatmate profile: " + ex.getMessage(), UNKNOWN_EXCEPTION);
        }
    }

    @Override
    public void upsertFromFilter(MatchFilterVO vo) throws MatchmakingException {
        try {
            FlatmateExtProfile profile = flatmateExtProfileRepository
                    .findByCognitoSub(vo.getCognitoSub())
                    .orElse(new FlatmateExtProfile());
            profile.setCognitoSub(vo.getCognitoSub());
            profile.setLookingIn(vo.getLookingIn());
            profile.setBudgetRangeInr(vo.getBudgetRangeInr());
            profile.setMoveInDate(vo.getMoveInDate());
            profile.setPreferredFlatmateGender(vo.getPreferredFlatmateGender());
            profile.setOccupationType(vo.getOccupationType());
            profile.setIsVegetarianHousehold(vo.getIsVegetarianHousehold());
            profile.setAllowsSmoking(vo.getAllowsSmoking());
            profile.setHasPets(vo.getHasPets());
            profile.setAllowsPets(vo.getAllowsPets());
            profile.setSleepSchedule(vo.getSleepSchedule());
            profile.setCleanlinessLevel(vo.getCleanlinessLevel());
            profile.setGuestsPolicy(vo.getGuestsPolicy());
            profile.setHasCurrentFlat(vo.getHasCurrentFlat());
            flatmateExtProfileRepository.save(profile);
            log.info("Flatmate ext profile upserted for cognitoSub: {}", vo.getCognitoSub());
        } catch (Exception ex) {
            log.error("ALERT_FOR_ERROR: Error upserting flatmate profile. Error: {}", ex.getMessage(), ex);
            throw new MatchmakingException("Error upserting flatmate profile: " + ex.getMessage(), UNKNOWN_EXCEPTION);
        }
    }
}