package com.shiviishiv7.matchmaking.processor.userprofile;

import com.shiviishiv7.matchmaking.common.exception.MatchmakingException;
import com.shiviishiv7.matchmaking.provider.implementation.DatingExtProfileRepository;
import com.shiviishiv7.matchmaking.provider.model.profile.DatingExtProfile;

import com.shiviishiv7.matchmaking.provider.vo.BaseVO;
import com.shiviishiv7.matchmaking.provider.vo.MatchFilterVO;
import com.shiviishiv7.matchmaking.provider.vo.DatingExtProfileVO;

import jakarta.transaction.Transactional;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;

import java.util.Optional;

import static com.shiviishiv7.matchmaking.common.constants.MatchmakingHttpStatus.*;

@Component
@Transactional
@Slf4j
public class DatingExtProfileProcessor implements IDatingExtProfileProcessor {

    @Autowired
    private DatingExtProfileRepository datingExtProfileRepository;

    @Override
    public BaseVO add(DatingExtProfileVO vo) throws MatchmakingException {
        try {
            log.info("Validating inputs for dating profile creation. {}", vo.toString());
            vo.validate();
            log.info("DatingExtProfileVO validation completed successfully.");

            log.trace("Checking for duplicate dating profile for userId: {}", vo.getCognitoSub());
            if (datingExtProfileRepository.existsByCognitoSub(vo.getCognitoSub())) {
                log.error("ALERT_FOR_ERROR: dating profile already exists for userId: {}", vo.getCognitoSub());
                throw new MatchmakingException("dating profile already exists for this user", DUPLICATE_RECORD);
            }

            log.trace("Saving dating profile for userId: {}", vo.getCognitoSub());
            DatingExtProfile profile = new DatingExtProfile();
            profile.fromVO(vo);
            profile = datingExtProfileRepository.save(profile);
            log.info("dating profile saved successfully for userId: {}", profile.getCognitoSub());

            return new BaseVO(SUCCESS, "dating profile created", "dating profile created", profile.toVO());
        } catch (MatchmakingException ex) {
            throw ex;
        } catch (Exception ex) {
            log.error("ALERT_FOR_ERROR: Error occurred while adding dating profile. Error: {}", ex.getMessage(), ex);
            throw new MatchmakingException("Error occurred while adding dating profile: " + ex.getMessage(), UNKNOWN_EXCEPTION);
        }
    }

    @Override
    public BaseVO update(DatingExtProfileVO vo) throws MatchmakingException {
        try {
            log.info("Validating inputs for dating profile update.");
            if (vo.getId() == null) {
                throw new MatchmakingException("DatingExtProfile ID cannot be null for update", VALIDATION_ERROR);
            }
            vo.validate();
            log.info("DatingExtProfileVO validation completed successfully.");

            log.trace("Fetching existing dating profile for ID: {}", vo.getId());
            Optional<DatingExtProfile> fromDB = datingExtProfileRepository.findById(vo.getId());
            if (fromDB.isEmpty()) {
                log.error("ALERT_FOR_ERROR: dating profile not found for ID: {}", vo.getId());
                throw new MatchmakingException("dating profile does not exist", DATA_NOT_FOUND);
            }

            DatingExtProfile profile = fromDB.get();
            log.trace("dating profile found for ID: {}. Applying updates.", vo.getId());

            profile.setDietaryHabits(vo.getDietaryHabits());
            profile.setSmokingHabit(vo.getSmokingHabit());
            profile.setDrinkingHabit(vo.getDrinkingHabit());
            profile.setHeightCm(vo.getHeightCm());
            profile.setBodyType(vo.getBodyType());
            profile.setRelationshipGoal(vo.getRelationshipGoal());
            profile.setSexualOrientation(vo.getSexualOrientation());
            profile.setHasChildren(vo.getHasChildren());
            profile.setWantsChildren(vo.getWantsChildren());
            profile.setLoveLanguage(vo.getLoveLanguage());
            profile.setPersonalityType(vo.getPersonalityType());
            profile.setInterestTags(vo.getInterestTags());
            profile.setPromptQuestion1(vo.getPromptQuestion1());
            profile.setPromptAnswer1(vo.getPromptAnswer1());
            profile.setPromptQuestion2(vo.getPromptQuestion2());
            profile.setPromptAnswer2(vo.getPromptAnswer2());
            profile.setPrefAgeMin(vo.getPrefAgeMin());
            profile.setPrefAgeMax(vo.getPrefAgeMax());
            profile.setPrefGenders(vo.getPrefGenders());
            profile.setPrefHeightMinCm(vo.getPrefHeightMinCm());
            profile.setPrefRelationshipGoal(vo.getPrefRelationshipGoal());

            profile = datingExtProfileRepository.save(profile);
            log.info("dating profile updated successfully for ID: {}", profile.getId());

            return new BaseVO(SUCCESS, "dating profile updated", "dating profile updated", profile.toVO());
        } catch (MatchmakingException ex) {
            throw ex;
        } catch (Exception ex) {
            log.error("ALERT_FOR_ERROR: Error occurred while updating dating profile. Error: {}", ex.getMessage(), ex);
            throw new MatchmakingException("Error occurred while updating dating profile: " + ex.getMessage(), UNKNOWN_EXCEPTION);
        }
    }

    @Override
    public BaseVO getById(String id) throws MatchmakingException {
        try {
            log.info("Fetching dating profile for ID: {}", id);
            Optional<DatingExtProfile> fromDB = datingExtProfileRepository.findById(Integer.valueOf(id));
            if (fromDB.isEmpty()) {
                log.error("ALERT_FOR_ERROR: dating profile not found for ID: {}", id);
                throw new MatchmakingException("dating profile does not exist", DATA_NOT_FOUND);
            }
            log.info("dating profile found for ID: {}", id);
            return new BaseVO(SUCCESS, "dating profile fetched", "dating profile fetched", fromDB.get().toVO());
        } catch (MatchmakingException ex) {
            throw ex;
        } catch (Exception ex) {
            log.error("ALERT_FOR_ERROR: Error occurred while fetching dating profile. Error: {}", ex.getMessage(), ex);
            throw new MatchmakingException("Error occurred while fetching dating profile: " + ex.getMessage(), UNKNOWN_EXCEPTION);
        }
    }

    @Override
    public BaseVO getByUserId(String userId) throws MatchmakingException {
        try {
            log.info("Fetching dating profile for userId: {}", userId);
            Optional<DatingExtProfile> fromDB = datingExtProfileRepository.findByCognitoSub(userId);
            if (fromDB.isEmpty()) {
                log.error("ALERT_FOR_ERROR: dating profile not found for userId: {}", userId);
                throw new MatchmakingException("dating profile does not exist for this user", DATA_NOT_FOUND);
            }
            log.info("dating profile found for userId: {}", userId);
            return new BaseVO(SUCCESS, "dating profile fetched", "dating profile fetched", fromDB.get().toVO());
        } catch (MatchmakingException ex) {
            throw ex;
        } catch (Exception ex) {
            log.error("ALERT_FOR_ERROR: Error occurred while fetching dating profile by userId. Error: {}", ex.getMessage(), ex);
            throw new MatchmakingException("Error occurred while fetching dating profile: " + ex.getMessage(), UNKNOWN_EXCEPTION);
        }
    }

    @Override
    public BaseVO delete(String id) throws MatchmakingException {
        try {
            log.info("Deleting dating profile for ID: {}", id);
            Optional<DatingExtProfile> fromDB = datingExtProfileRepository.findById(Integer.valueOf(id));
            if (fromDB.isEmpty()) {
                log.error("ALERT_FOR_ERROR: dating profile not found for ID: {}", id);
                throw new MatchmakingException("dating profile does not exist", DATA_NOT_FOUND);
            }
            datingExtProfileRepository.deleteById(Integer.valueOf(id));
            log.info("dating profile hard-deleted for ID: {}", id);
            return new BaseVO(SUCCESS, "dating profile deleted", "dating profile deleted");
        } catch (MatchmakingException ex) {
            throw ex;
        } catch (Exception ex) {
            log.error("ALERT_FOR_ERROR: Error occurred while deleting dating profile. Error: {}", ex.getMessage(), ex);
            throw new MatchmakingException("Error occurred while deleting dating profile: " + ex.getMessage(), UNKNOWN_EXCEPTION);
        }
    }

    @Override
    public void upsertFromFilter(MatchFilterVO vo) throws MatchmakingException {
        try {
            DatingExtProfile profile = datingExtProfileRepository
                    .findByCognitoSub(vo.getCognitoSub())
                    .orElse(new DatingExtProfile());
            profile.setCognitoSub(vo.getCognitoSub());
            profile.setDietaryHabits(vo.getDietaryHabits());
            profile.setSmokingHabit(vo.getSmokingHabit());
            profile.setDrinkingHabit(vo.getDrinkingHabit());
            profile.setHeightCm(vo.getHeightCm());
            profile.setBodyType(vo.getBodyType());
            profile.setRelationshipGoal(vo.getRelationshipGoal());
            profile.setSexualOrientation(vo.getSexualOrientation());
            profile.setHasChildren(vo.getHasChildren());
            profile.setWantsChildren(vo.getWantsChildren());
            profile.setLoveLanguage(vo.getLoveLanguage());
            profile.setPersonalityType(vo.getPersonalityType());
            profile.setPrefHeightMinCm(vo.getPrefHeightMinCm());
            datingExtProfileRepository.save(profile);
            log.info("Dating ext profile upserted for cognitoSub: {}", vo.getCognitoSub());
        } catch (Exception ex) {
            log.error("ALERT_FOR_ERROR: Error upserting dating profile. Error: {}", ex.getMessage(), ex);
            throw new MatchmakingException("Error upserting dating profile: " + ex.getMessage(), UNKNOWN_EXCEPTION);
        }
    }
}
