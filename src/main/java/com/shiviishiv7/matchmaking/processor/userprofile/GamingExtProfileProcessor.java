package com.shiviishiv7.matchmaking.processor.userprofile;

import com.shiviishiv7.matchmaking.common.exception.MatchmakingException;
import com.shiviishiv7.matchmaking.provider.implementation.GamingExtProfileRepository;
import com.shiviishiv7.matchmaking.provider.model.profile.GamingExtProfile;

import com.shiviishiv7.matchmaking.provider.vo.BaseVO;
import com.shiviishiv7.matchmaking.provider.vo.MatchFilterVO;
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

            log.trace("Checking for duplicate gaming profile for userId: {}", vo.getCognitoSub());
            if (gamingExtProfileRepository.existsByCognitoSub(vo.getCognitoSub())) {
                log.error("ALERT_FOR_ERROR: gaming profile already exists for userId: {}", vo.getCognitoSub());
                throw new MatchmakingException("gaming profile already exists for this user", DUPLICATE_RECORD);
            }

            log.trace("Saving gaming profile for userId: {}", vo.getCognitoSub());
            GamingExtProfile profile = new GamingExtProfile();
            profile.fromVO(vo);
            profile = gamingExtProfileRepository.save(profile);
            log.info("gaming profile saved successfully for userId: {}", profile.getCognitoSub());

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
            if (vo.getCognitoSub() == null) {
                throw new MatchmakingException("cognitoSub cannot be null for update", VALIDATION_ERROR);
            }
            vo.validate();
            log.info("GamingExtProfileVO validation completed successfully.");

            log.trace("Fetching profile for cognitoSub: {}", vo.getId());
            Optional<GamingExtProfile> fromDB = gamingExtProfileRepository.findByCognitoSub(vo.getCognitoSub());
            if (fromDB.isEmpty()) {
                log.error("ALERT_FOR_ERROR: gaming profile not found for cognitoSub: {}", vo.getId());
                throw new MatchmakingException("gaming profile does not exist", DATA_NOT_FOUND);
            }

            GamingExtProfile profile = fromDB.get();
            log.trace("gaming profile found for cognitoSub: {}. Applying updates.", vo.getId());

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
    public BaseVO getByCognitoSub(String cognitoSub) throws MatchmakingException {
        try {
            log.info("Fetching profile for cognitoSub: {}", cognitoSub);
            Optional<GamingExtProfile> fromDB = gamingExtProfileRepository.findByCognitoSub(cognitoSub);
            if (fromDB.isEmpty()) {
                log.error("ALERT_FOR_ERROR: profile not found for cognitoSub: {}", cognitoSub);
                throw new MatchmakingException("gaming profile does not exist", DATA_NOT_FOUND);
            }
            log.info("Profile found for cognitoSub: {}", cognitoSub);
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
            Optional<GamingExtProfile> fromDB = gamingExtProfileRepository.findByCognitoSub(userId);
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
    public BaseVO delete(String cognitoSub) throws MatchmakingException {
        try {
            log.info("Deleting gaming profile for cognitoSub: {}", cognitoSub);
            Optional<GamingExtProfile> fromDB = gamingExtProfileRepository.findByCognitoSub(cognitoSub);
            if (fromDB.isEmpty()) {
                log.error("ALERT_FOR_ERROR: profile not found for cognitoSub: {}", cognitoSub);
                throw new MatchmakingException("gaming profile does not exist", DATA_NOT_FOUND);
            }
            gamingExtProfileRepository.delete(fromDB.get());
            log.info("gaming profile hard-deleted for cognitoSub: {}", cognitoSub);
            return new BaseVO(SUCCESS, "gaming profile deleted", "gaming profile deleted");
        } catch (MatchmakingException ex) {
            throw ex;
        } catch (Exception ex) {
            log.error("ALERT_FOR_ERROR: Error occurred while deleting gaming profile. Error: {}", ex.getMessage(), ex);
            throw new MatchmakingException("Error occurred while deleting gaming profile: " + ex.getMessage(), UNKNOWN_EXCEPTION);
        }
    }

    @Override
    public void upsertFromFilter(MatchFilterVO vo) throws MatchmakingException {
        try {
            GamingExtProfile profile = gamingExtProfileRepository
                    .findByCognitoSub(vo.getCognitoSub())
                    .orElse(new GamingExtProfile());
            profile.setCognitoSub(vo.getCognitoSub());
            profile.setPlatforms(vo.getPlatforms());
            profile.setFavoriteGames(vo.getFavoriteGames());
            profile.setFavoriteGenres(vo.getFavoriteGenres());
            profile.setGamingSchedule(vo.getGamingSchedule());
            profile.setSkillLevel(vo.getSkillLevel());
            profile.setCommunicationStyle(vo.getCommunicationStyle());
            profile.setIsOkWithNewbies(vo.getIsOkWithNewbies());
            gamingExtProfileRepository.save(profile);
            log.info("Gaming ext profile upserted for cognitoSub: {}", vo.getCognitoSub());
        } catch (Exception ex) {
            log.error("ALERT_FOR_ERROR: Error upserting gaming profile. Error: {}", ex.getMessage(), ex);
            throw new MatchmakingException("Error upserting gaming profile: " + ex.getMessage(), UNKNOWN_EXCEPTION);
        }
    }
}