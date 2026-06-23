package com.shiviishiv7.matchmaking.processor.userprofile;

import com.shiviishiv7.matchmaking.common.exception.MatchmakingException;
import com.shiviishiv7.matchmaking.provider.implementation.FitnessExtProfileRepository;
import com.shiviishiv7.matchmaking.provider.model.profile.FitnessExtProfile;

import com.shiviishiv7.matchmaking.provider.vo.BaseVO;
import com.shiviishiv7.matchmaking.provider.vo.MatchFilterVO;
import com.shiviishiv7.matchmaking.provider.vo.FitnessExtProfileVO;

import jakarta.transaction.Transactional;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;

import java.util.Optional;

import static com.shiviishiv7.matchmaking.common.constants.MatchmakingHttpStatus.*;

@Component
@Transactional
@Slf4j
public class FitnessExtProfileProcessor implements IFitnessExtProfileProcessor {

    @Autowired
    private FitnessExtProfileRepository fitnessExtProfileRepository;

    @Override
    public BaseVO add(FitnessExtProfileVO vo) throws MatchmakingException {
        try {
            log.info("Validating inputs for fitness profile creation. {}", vo.toString());
            vo.validate();
            log.info("FitnessExtProfileVO validation completed successfully.");

            log.trace("Checking for duplicate fitness profile for userId: {}", vo.getCognitoSub());
            if (fitnessExtProfileRepository.existsByCognitoSub(vo.getCognitoSub())) {
                log.error("ALERT_FOR_ERROR: fitness profile already exists for userId: {}", vo.getCognitoSub());
                throw new MatchmakingException("fitness profile already exists for this user", DUPLICATE_RECORD);
            }

            log.trace("Saving fitness profile for userId: {}", vo.getCognitoSub());
            FitnessExtProfile profile = new FitnessExtProfile();
            profile.fromVO(vo);
            profile = fitnessExtProfileRepository.save(profile);
            log.info("fitness profile saved successfully for userId: {}", profile.getCognitoSub());

            return new BaseVO(SUCCESS, "fitness profile created", "fitness profile created", profile.toVO());
        } catch (MatchmakingException ex) {
            throw ex;
        } catch (Exception ex) {
            log.error("ALERT_FOR_ERROR: Error occurred while adding fitness profile. Error: {}", ex.getMessage(), ex);
            throw new MatchmakingException("Error occurred while adding fitness profile: " + ex.getMessage(), UNKNOWN_EXCEPTION);
        }
    }

    @Override
    public BaseVO update(FitnessExtProfileVO vo) throws MatchmakingException {
        try {
            log.info("Validating inputs for fitness profile update.");
            if (vo.getCognitoSub() == null) {
                throw new MatchmakingException("cognitoSub cannot be null for update", VALIDATION_ERROR);
            }
            vo.validate();
            log.info("FitnessExtProfileVO validation completed successfully.");

            log.trace("Fetching profile for cognitoSub: {}", vo.getId());
            Optional<FitnessExtProfile> fromDB = fitnessExtProfileRepository.findByCognitoSub(vo.getCognitoSub());
            if (fromDB.isEmpty()) {
                log.error("ALERT_FOR_ERROR: fitness profile not found for cognitoSub: {}", vo.getId());
                throw new MatchmakingException("fitness profile does not exist", DATA_NOT_FOUND);
            }

            FitnessExtProfile profile = fromDB.get();
            log.trace("fitness profile found for cognitoSub: {}. Applying updates.", vo.getId());

            profile.setFitnessActivities(vo.getFitnessActivities());
            profile.setFitnessLevel(vo.getFitnessLevel());
            profile.setWorkoutDays(vo.getWorkoutDays());
            profile.setPreferredWorkoutTime(vo.getPreferredWorkoutTime());
            profile.setGymName(vo.getGymName());
            profile.setIsOkWithMixedGender(vo.getIsOkWithMixedGender());
            profile.setSportsLeagueLevel(vo.getSportsLeagueLevel());
            profile.setFitnessGoal(vo.getFitnessGoal());
            profile.setDietPreference(vo.getDietPreference());

            profile = fitnessExtProfileRepository.save(profile);
            log.info("fitness profile updated successfully for ID: {}", profile.getId());

            return new BaseVO(SUCCESS, "fitness profile updated", "fitness profile updated", profile.toVO());
        } catch (MatchmakingException ex) {
            throw ex;
        } catch (Exception ex) {
            log.error("ALERT_FOR_ERROR: Error occurred while updating fitness profile. Error: {}", ex.getMessage(), ex);
            throw new MatchmakingException("Error occurred while updating fitness profile: " + ex.getMessage(), UNKNOWN_EXCEPTION);
        }
    }

    @Override
    public BaseVO getByCognitoSub(String cognitoSub) throws MatchmakingException {
        try {
            log.info("Fetching profile for cognitoSub: {}", cognitoSub);
            Optional<FitnessExtProfile> fromDB = fitnessExtProfileRepository.findByCognitoSub(cognitoSub);
            if (fromDB.isEmpty()) {
                log.error("ALERT_FOR_ERROR: profile not found for cognitoSub: {}", cognitoSub);
                throw new MatchmakingException("fitness profile does not exist", DATA_NOT_FOUND);
            }
            log.info("Profile found for cognitoSub: {}", cognitoSub);
            return new BaseVO(SUCCESS, "fitness profile fetched", "fitness profile fetched", fromDB.get().toVO());
        } catch (MatchmakingException ex) {
            throw ex;
        } catch (Exception ex) {
            log.error("ALERT_FOR_ERROR: Error occurred while fetching fitness profile. Error: {}", ex.getMessage(), ex);
            throw new MatchmakingException("Error occurred while fetching fitness profile: " + ex.getMessage(), UNKNOWN_EXCEPTION);
        }
    }

    @Override
    public BaseVO getByUserId(String userId) throws MatchmakingException {
        try {
            log.info("Fetching fitness profile for userId: {}", userId);
            Optional<FitnessExtProfile> fromDB = fitnessExtProfileRepository.findByCognitoSub(userId);
            if (fromDB.isEmpty()) {
                log.error("ALERT_FOR_ERROR: fitness profile not found for userId: {}", userId);
                throw new MatchmakingException("fitness profile does not exist for this user", DATA_NOT_FOUND);
            }
            log.info("fitness profile found for userId: {}", userId);
            return new BaseVO(SUCCESS, "fitness profile fetched", "fitness profile fetched", fromDB.get().toVO());
        } catch (MatchmakingException ex) {
            throw ex;
        } catch (Exception ex) {
            log.error("ALERT_FOR_ERROR: Error occurred while fetching fitness profile by userId. Error: {}", ex.getMessage(), ex);
            throw new MatchmakingException("Error occurred while fetching fitness profile: " + ex.getMessage(), UNKNOWN_EXCEPTION);
        }
    }

    @Override
    public BaseVO delete(String cognitoSub) throws MatchmakingException {
        try {
            log.info("Deleting fitness profile for cognitoSub: {}", cognitoSub);
            Optional<FitnessExtProfile> fromDB = fitnessExtProfileRepository.findByCognitoSub(cognitoSub);
            if (fromDB.isEmpty()) {
                log.error("ALERT_FOR_ERROR: profile not found for cognitoSub: {}", cognitoSub);
                throw new MatchmakingException("fitness profile does not exist", DATA_NOT_FOUND);
            }
            fitnessExtProfileRepository.delete(fromDB.get());
            log.info("fitness profile hard-deleted for cognitoSub: {}", cognitoSub);
            return new BaseVO(SUCCESS, "fitness profile deleted", "fitness profile deleted");
        } catch (MatchmakingException ex) {
            throw ex;
        } catch (Exception ex) {
            log.error("ALERT_FOR_ERROR: Error occurred while deleting fitness profile. Error: {}", ex.getMessage(), ex);
            throw new MatchmakingException("Error occurred while deleting fitness profile: " + ex.getMessage(), UNKNOWN_EXCEPTION);
        }
    }

    @Override
    public void upsertFromFilter(MatchFilterVO vo) throws MatchmakingException {
        try {
            FitnessExtProfile profile = fitnessExtProfileRepository
                    .findByCognitoSub(vo.getCognitoSub())
                    .orElse(new FitnessExtProfile());
            profile.setCognitoSub(vo.getCognitoSub());
            profile.setFitnessActivities(vo.getFitnessActivities());
            profile.setFitnessLevel(vo.getFitnessLevel());
            profile.setWorkoutDays(vo.getWorkoutDays());
            profile.setPreferredWorkoutTime(vo.getPreferredWorkoutTime());
            profile.setGymName(vo.getGymName());
            profile.setIsOkWithMixedGender(vo.getIsOkWithMixedGender());
            profile.setSportsLeagueLevel(vo.getSportsLeagueLevel());
            profile.setFitnessGoal(vo.getFitnessGoal());
            profile.setDietPreference(vo.getDietPreference());
            fitnessExtProfileRepository.save(profile);
            log.info("Fitness ext profile upserted for cognitoSub: {}", vo.getCognitoSub());
        } catch (Exception ex) {
            log.error("ALERT_FOR_ERROR: Error upserting fitness profile. Error: {}", ex.getMessage(), ex);
            throw new MatchmakingException("Error upserting fitness profile: " + ex.getMessage(), UNKNOWN_EXCEPTION);
        }
    }
}