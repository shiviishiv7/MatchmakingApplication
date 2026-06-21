package com.shiviishiv7.matchmaking.processor.userprofile;

import com.shiviishiv7.matchmaking.common.exception.MatchmakingException;
import com.shiviishiv7.matchmaking.provider.implementation.GamingExtProfileRepository;
import com.shiviishiv7.matchmaking.provider.model.GamingExtProfile;

import com.shiviishiv7.matchmaking.provider.vo.BaseVO;
import com.shiviishiv7.matchmaking.provider.vo.GamingExtProfileVO;

import jakarta.transaction.Transactional;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;

import java.util.Optional;

import static com.shiviishiv7.matchmaking.common.constants.MatchmakingHttpStatus.*;

@Component
@Transactional
@Slf4j
public class GamingExtProfileProcessor implements IGamingExtProfileProcessor {

    @Autowired
    private GamingExtProfileRepository gamingExtProfileRepository;

    @Override
    public BaseVO add(GamingExtProfileVO vo) throws MatchmakingException {
        try {
            log.info("Validating inputs for gaming profile creation. {}", vo.toString());
            vo.validate();
            log.info("GamingExtProfileVO validation completed successfully.");

            log.trace("Checking for duplicate gaming profile for userId: {}", vo.getUserId());
            if (gamingExtProfileRepository.existsByUserId(vo.getUserId())) {
                log.error("ALERT_FOR_ERROR: gaming profile already exists for userId: {}", vo.getUserId());
                throw new MatchmakingException("gaming profile already exists for this user", DUPLICATE_RECORD);
            }

            log.trace("Saving gaming profile for userId: {}", vo.getUserId());
            GamingExtProfile profile = new GamingExtProfile();
            profile.fromVO(vo);
            profile = gamingExtProfileRepository.save(profile);
            log.info("gaming profile saved successfully for userId: {}", profile.getUserId());

            return new BaseVO(SUCCESS, "gaming profile created", "gaming profile created", profile.toVO());
        } catch (MatchmakingException ex) {
            throw ex;
        } catch (Exception ex) {
            log.error("ALERT_FOR_ERROR: Error occurred while adding gaming profile. Error: {}", ex.getMessage(), ex);
            throw new MatchmakingException("Error occurred while adding gaming profile: " + ex.getMessage(), UNKNOWN_EXCEPTION);
        }
    }

    @Override
    public BaseVO update(GamingExtProfileVO vo) throws MatchmakingException {
        try {
            log.info("Validating inputs for gaming profile update.");
            if (vo.getId() == null) {
                throw new MatchmakingException("GamingExtProfile ID cannot be null for update", VALIDATION_ERROR);
            }
            vo.validate();
            log.info("GamingExtProfileVO validation completed successfully.");

            log.trace("Fetching existing gaming profile for ID: {}", vo.getId());
            Optional<GamingExtProfile> fromDB = gamingExtProfileRepository.findById(vo.getId());
            if (fromDB.isEmpty()) {
                log.error("ALERT_FOR_ERROR: gaming profile not found for ID: {}", vo.getId());
                throw new MatchmakingException("gaming profile does not exist", DATA_NOT_FOUND);
            }

            GamingExtProfile profile = fromDB.get();
            log.trace("gaming profile found for ID: {}. Applying updates.", vo.getId());

            profile.setPlatforms(vo.getPlatforms());
            profile.setFavoriteGames(vo.getFavoriteGames());
            profile.setFavoriteGenres(vo.getFavoriteGenres());
            profile.setGamingSchedule(vo.getGamingSchedule());
            profile.setSkillLevel(vo.getSkillLevel());
            profile.setCommunicationStyle(vo.getCommunicationStyle());
            profile.setIsOkWithNewbies(vo.getIsOkWithNewbies());
            profile.setGamertags(vo.getGamertags());

            profile = gamingExtProfileRepository.save(profile);
            log.info("gaming profile updated successfully for ID: {}", profile.getId());

            return new BaseVO(SUCCESS, "gaming profile updated", "gaming profile updated", profile.toVO());
        } catch (MatchmakingException ex) {
            throw ex;
        } catch (Exception ex) {
            log.error("ALERT_FOR_ERROR: Error occurred while updating gaming profile. Error: {}", ex.getMessage(), ex);
            throw new MatchmakingException("Error occurred while updating gaming profile: " + ex.getMessage(), UNKNOWN_EXCEPTION);
        }
    }

    @Override
    public BaseVO getById(String id) throws MatchmakingException {
        try {
            log.info("Fetching gaming profile for ID: {}", id);
            Optional<GamingExtProfile> fromDB = gamingExtProfileRepository.findById(Integer.valueOf(id));
            if (fromDB.isEmpty()) {
                log.error("ALERT_FOR_ERROR: gaming profile not found for ID: {}", id);
                throw new MatchmakingException("gaming profile does not exist", DATA_NOT_FOUND);
            }
            log.info("gaming profile found for ID: {}", id);
            return new BaseVO(SUCCESS, "gaming profile fetched", "gaming profile fetched", fromDB.get().toVO());
        } catch (MatchmakingException ex) {
            throw ex;
        } catch (Exception ex) {
            log.error("ALERT_FOR_ERROR: Error occurred while fetching gaming profile. Error: {}", ex.getMessage(), ex);
            throw new MatchmakingException("Error occurred while fetching gaming profile: " + ex.getMessage(), UNKNOWN_EXCEPTION);
        }
    }

    @Override
    public BaseVO getByUserId(String userId) throws MatchmakingException {
        try {
            log.info("Fetching gaming profile for userId: {}", userId);
            Optional<GamingExtProfile> fromDB = gamingExtProfileRepository.findByUserId(Integer.valueOf(userId));
            if (fromDB.isEmpty()) {
                log.error("ALERT_FOR_ERROR: gaming profile not found for userId: {}", userId);
                throw new MatchmakingException("gaming profile does not exist for this user", DATA_NOT_FOUND);
            }
            log.info("gaming profile found for userId: {}", userId);
            return new BaseVO(SUCCESS, "gaming profile fetched", "gaming profile fetched", fromDB.get().toVO());
        } catch (MatchmakingException ex) {
            throw ex;
        } catch (Exception ex) {
            log.error("ALERT_FOR_ERROR: Error occurred while fetching gaming profile by userId. Error: {}", ex.getMessage(), ex);
            throw new MatchmakingException("Error occurred while fetching gaming profile: " + ex.getMessage(), UNKNOWN_EXCEPTION);
        }
    }

    @Override
    public BaseVO delete(String id) throws MatchmakingException {
        try {
            log.info("Deleting gaming profile for ID: {}", id);
            Optional<GamingExtProfile> fromDB = gamingExtProfileRepository.findById(Integer.valueOf(id));
            if (fromDB.isEmpty()) {
                log.error("ALERT_FOR_ERROR: gaming profile not found for ID: {}", id);
                throw new MatchmakingException("gaming profile does not exist", DATA_NOT_FOUND);
            }
            gamingExtProfileRepository.deleteById(Integer.valueOf(id));
            log.info("gaming profile hard-deleted for ID: {}", id);
            return new BaseVO(SUCCESS, "gaming profile deleted", "gaming profile deleted");
        } catch (MatchmakingException ex) {
            throw ex;
        } catch (Exception ex) {
            log.error("ALERT_FOR_ERROR: Error occurred while deleting gaming profile. Error: {}", ex.getMessage(), ex);
            throw new MatchmakingException("Error occurred while deleting gaming profile: " + ex.getMessage(), UNKNOWN_EXCEPTION);
        }
    }
}
