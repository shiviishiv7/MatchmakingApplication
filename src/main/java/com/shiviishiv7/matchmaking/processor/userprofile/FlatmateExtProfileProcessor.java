package com.shiviishiv7.matchmaking.processor.userprofile;

import com.shiviishiv7.matchmaking.common.exception.MatchmakingException;
import com.shiviishiv7.matchmaking.provider.implementation.FlatmateExtProfileRepository;
import com.shiviishiv7.matchmaking.provider.model.profile.FlatmateExtProfile;

import com.shiviishiv7.matchmaking.provider.vo.BaseVO;
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

            log.trace("Checking for duplicate flatmate profile for userId: {}", vo.getUserId());
            if (flatmateExtProfileRepository.existsByUserId(vo.getUserId())) {
                log.error("ALERT_FOR_ERROR: flatmate profile already exists for userId: {}", vo.getUserId());
                throw new MatchmakingException("flatmate profile already exists for this user", DUPLICATE_RECORD);
            }

            log.trace("Saving flatmate profile for userId: {}", vo.getUserId());
            FlatmateExtProfile profile = new FlatmateExtProfile();
            profile.fromVO(vo);
            profile = flatmateExtProfileRepository.save(profile);
            log.info("flatmate profile saved successfully for userId: {}", profile.getUserId());

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
            if (vo.getId() == null) {
                throw new MatchmakingException("FlatmateExtProfile ID cannot be null for update", VALIDATION_ERROR);
            }
            vo.validate();
            log.info("FlatmateExtProfileVO validation completed successfully.");

            log.trace("Fetching existing flatmate profile for ID: {}", vo.getId());
            Optional<FlatmateExtProfile> fromDB = flatmateExtProfileRepository.findById(vo.getId());
            if (fromDB.isEmpty()) {
                log.error("ALERT_FOR_ERROR: flatmate profile not found for ID: {}", vo.getId());
                throw new MatchmakingException("flatmate profile does not exist", DATA_NOT_FOUND);
            }

            FlatmateExtProfile profile = fromDB.get();
            log.trace("flatmate profile found for ID: {}. Applying updates.", vo.getId());

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
    public BaseVO getById(String id) throws MatchmakingException {
        try {
            log.info("Fetching flatmate profile for ID: {}", id);
            Optional<FlatmateExtProfile> fromDB = flatmateExtProfileRepository.findById(Integer.valueOf(id));
            if (fromDB.isEmpty()) {
                log.error("ALERT_FOR_ERROR: flatmate profile not found for ID: {}", id);
                throw new MatchmakingException("flatmate profile does not exist", DATA_NOT_FOUND);
            }
            log.info("flatmate profile found for ID: {}", id);
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
            Optional<FlatmateExtProfile> fromDB = flatmateExtProfileRepository.findByUserId(Integer.valueOf(userId));
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
    public BaseVO delete(String id) throws MatchmakingException {
        try {
            log.info("Deleting flatmate profile for ID: {}", id);
            Optional<FlatmateExtProfile> fromDB = flatmateExtProfileRepository.findById(Integer.valueOf(id));
            if (fromDB.isEmpty()) {
                log.error("ALERT_FOR_ERROR: flatmate profile not found for ID: {}", id);
                throw new MatchmakingException("flatmate profile does not exist", DATA_NOT_FOUND);
            }
            flatmateExtProfileRepository.deleteById(Integer.valueOf(id));
            log.info("flatmate profile hard-deleted for ID: {}", id);
            return new BaseVO(SUCCESS, "flatmate profile deleted", "flatmate profile deleted");
        } catch (MatchmakingException ex) {
            throw ex;
        } catch (Exception ex) {
            log.error("ALERT_FOR_ERROR: Error occurred while deleting flatmate profile. Error: {}", ex.getMessage(), ex);
            throw new MatchmakingException("Error occurred while deleting flatmate profile: " + ex.getMessage(), UNKNOWN_EXCEPTION);
        }
    }
}
