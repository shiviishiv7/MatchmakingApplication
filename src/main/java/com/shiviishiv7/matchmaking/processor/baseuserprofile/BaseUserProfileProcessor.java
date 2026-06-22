package com.shiviishiv7.matchmaking.processor.baseuserprofile;

import com.shiviishiv7.matchmaking.common.exception.MatchmakingException;
import com.shiviishiv7.matchmaking.provider.implementation.BaseUserProfileRepository;
import com.shiviishiv7.matchmaking.provider.model.profile.BaseUserProfile;

import com.shiviishiv7.matchmaking.provider.model.User;
import com.shiviishiv7.matchmaking.provider.vo.BaseUserProfileVO;
import com.shiviishiv7.matchmaking.provider.vo.BaseVO;
import jakarta.transaction.Transactional;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;

import java.util.Optional;

import static com.shiviishiv7.matchmaking.common.constants.MatchmakingHttpStatus.*;

@Component
@Transactional
@Slf4j
public class BaseUserProfileProcessor implements IBaseUserProfileProcessor {

    @Autowired
    private BaseUserProfileRepository baseUserProfileRepository;

    @Override
    public BaseVO add(BaseUserProfileVO vo) throws MatchmakingException {
        try {
            log.info("Validating inputs for base profile creation. {}", vo.toString());
            vo.validate();
            log.info("BaseUserProfileVO validation completed successfully.");

            log.trace("Checking for duplicate base profile for userId: {}", vo.getCognitoSub());
            if (baseUserProfileRepository.existsByCognitoSub(vo.getCognitoSub())) {
                log.error("ALERT_FOR_ERROR: Base profile already exists for userId: {}", vo.getCognitoSub());
                throw new MatchmakingException("Base profile already exists for this user", DUPLICATE_RECORD);
            }

            log.trace("Saving base profile for userId: {}", vo.getCognitoSub());
            BaseUserProfile profile = new BaseUserProfile();
            profile.fromVO(vo);
            profile = baseUserProfileRepository.save(profile);
            log.info("Base profile saved successfully for userId: {}", profile.getCognitoSub());

            return new BaseVO(SUCCESS, "Base profile created", "Base profile created", profile.toVO());
        } catch (MatchmakingException ex) {
            throw ex;
        } catch (Exception ex) {
            log.error("ALERT_FOR_ERROR: Error occurred while adding base profile. Error: {}", ex.getMessage(), ex);
            throw new MatchmakingException("Error occurred while adding base profile: " + ex.getMessage(), UNKNOWN_EXCEPTION);
        }
    }

    @Override
    public BaseVO update(BaseUserProfileVO vo) throws MatchmakingException {
        try {
            log.info("Validating inputs for base profile update.");
            if (vo.getId() == null) {
                throw new MatchmakingException("Profile ID cannot be null for update", VALIDATION_ERROR);
            }
            vo.validate();
            log.info("BaseUserProfileVO validation completed successfully.");

            log.trace("Fetching existing base profile for ID: {}", vo.getId());
            Optional<BaseUserProfile> fromDB = baseUserProfileRepository.findById(vo.getId());
            if (fromDB.isEmpty()) {
                log.error("ALERT_FOR_ERROR: Base profile not found for ID: {}", vo.getId());
                throw new MatchmakingException("Base profile does not exist", DATA_NOT_FOUND);
            }

            BaseUserProfile profile = fromDB.get();
            log.trace("Base profile found for ID: {}. Applying updates.", vo.getId());

            profile.setName(vo.getName());
            profile.setDateOfBirth(vo.getDateOfBirth());
            profile.setGender(vo.getGender());
            profile.setCurrentCity(vo.getCurrentCity());
            profile.setCurrentState(vo.getCurrentState());
            profile.setCurrentCountry(vo.getCurrentCountry());
            profile.setLatitude(vo.getLatitude());
            profile.setLongitude(vo.getLongitude());
            profile.setProfilePhotoUrl(vo.getProfilePhotoUrl());
            profile.setTagline(vo.getTagline());
            profile.setAboutMe(vo.getAboutMe());
            profile.setLanguagesKnown(vo.getLanguagesKnown());
            if (vo.getIsActive() != null) {
                profile.setIsActive(vo.getIsActive());
            }

            profile = baseUserProfileRepository.save(profile);
            log.info("Base profile updated successfully for ID: {}", profile.getId());

            return new BaseVO(SUCCESS, "Base profile updated", "Base profile updated", profile.toVO());
        } catch (MatchmakingException ex) {
            throw ex;
        } catch (Exception ex) {
            log.error("ALERT_FOR_ERROR: Error occurred while updating base profile. Error: {}", ex.getMessage(), ex);
            throw new MatchmakingException("Error occurred while updating base profile: " + ex.getMessage(), UNKNOWN_EXCEPTION);
        }
    }

    @Override
    public BaseVO getById(String id) throws MatchmakingException {
        try {
            log.info("Fetching base profile for ID: {}", id);
            Optional<BaseUserProfile> fromDB = baseUserProfileRepository.findById(Integer.valueOf(id));
            if (fromDB.isEmpty()) {
                log.error("ALERT_FOR_ERROR: Base profile not found for ID: {}", id);
                throw new MatchmakingException("Base profile does not exist", DATA_NOT_FOUND);
            }
            log.info("Base profile found for ID: {}", id);
            return new BaseVO(SUCCESS, "Base profile fetched", "Base profile fetched", fromDB.get().toVO());
        } catch (MatchmakingException ex) {
            throw ex;
        } catch (Exception ex) {
            log.error("ALERT_FOR_ERROR: Error occurred while fetching base profile. Error: {}", ex.getMessage(), ex);
            throw new MatchmakingException("Error occurred while fetching base profile: " + ex.getMessage(), UNKNOWN_EXCEPTION);
        }
    }

    @Override
    public BaseVO getByUserId(String userId) throws MatchmakingException {
        try {
            log.info("Fetching base profile for userId: {}", userId);
            Optional<BaseUserProfile> fromDB = baseUserProfileRepository.findByCognitoSub((userId));
            if (fromDB.isEmpty()) {
                log.error("ALERT_FOR_ERROR: Base profile not found for userId: {}", userId);
                throw new MatchmakingException("Base profile does not exist for this user", DATA_NOT_FOUND);
            }
            log.info("Base profile found for userId: {}", userId);
            return new BaseVO(SUCCESS, "Base profile fetched", "Base profile fetched", fromDB.get().toVO());
        } catch (MatchmakingException ex) {
            throw ex;
        } catch (Exception ex) {
            log.error("ALERT_FOR_ERROR: Error occurred while fetching base profile by userId. Error: {}", ex.getMessage(), ex);
            throw new MatchmakingException("Error occurred while fetching base profile: " + ex.getMessage(), UNKNOWN_EXCEPTION);
        }
    }

    @Override
    public BaseVO delete(String id) throws MatchmakingException {
        try {
            log.info("Deactivating base profile for ID: {}", id);
            Optional<BaseUserProfile> fromDB = baseUserProfileRepository.findById(Integer.valueOf(id));
            if (fromDB.isEmpty()) {
                log.error("ALERT_FOR_ERROR: Base profile not found for ID: {}", id);
                throw new MatchmakingException("Base profile does not exist", DATA_NOT_FOUND);
            }
            BaseUserProfile profile = fromDB.get();
            profile.setIsActive(false);
            baseUserProfileRepository.save(profile);
            log.info("Base profile soft-deleted (deactivated) for ID: {}", id);
            return new BaseVO(SUCCESS, "Base profile deactivated", "Base profile deactivated");
        } catch (MatchmakingException ex) {
            throw ex;
        } catch (Exception ex) {
            log.error("ALERT_FOR_ERROR: Error occurred while deleting base profile. Error: {}", ex.getMessage(), ex);
            throw new MatchmakingException("Error occurred while deleting base profile: " + ex.getMessage(), UNKNOWN_EXCEPTION);
        }
    }
    @Override
    public BaseVO get(String id) throws MatchmakingException {
        try {
            log.info("Fetching user for ID: {}", id);
            Optional<BaseUserProfile> optionalUser = baseUserProfileRepository.findById(Integer.valueOf(id));
            if (optionalUser.isEmpty()) {
                log.error("ALERT_FOR_ERROR: User not found for ID: {}", id);
                throw new MatchmakingException("User does not exist", DATA_NOT_FOUND);
            }
            BaseUserProfileVO vo = optionalUser.get().toVO();
            log.info("User found for ID: {}", id);
            return new BaseVO(SUCCESS, "User record fetched", "User record fetched", vo);
        } catch (MatchmakingException ex) {
            throw ex;
        } catch (Exception ex) {
            log.error("ALERT_FOR_ERROR: Error occurred while fetching user. Error: {}", ex.getMessage(), ex);
            throw new MatchmakingException("Error occurred while fetching user: " + ex.getMessage(), UNKNOWN_EXCEPTION);
        }
    }

    @Override
    public BaseVO getByEmail(String email) throws MatchmakingException {
        try {
            log.info("Fetching user for email: {}", email);
            if (email == null || email.isBlank()) {
                throw new MatchmakingException("Email cannot be empty", VALIDATION_ERROR);
            }

            Optional<BaseUserProfile> optionalUser = baseUserProfileRepository.findByEmail(email);
            if (optionalUser.isEmpty()) {
                log.error("ALERT_FOR_ERROR: User not found for email: {}", email);
                throw new MatchmakingException("User does not exist", DATA_NOT_FOUND);
            }
            BaseUserProfileVO vo = optionalUser.get().toVO();

            log.info("User found for email: {}", email);
            return new BaseVO(SUCCESS, "User record fetched", "User record fetched", vo);
        } catch (MatchmakingException ex) {
            throw ex;
        } catch (Exception ex) {
            log.error("ALERT_FOR_ERROR: Error occurred while fetching user by email. Error: {}", ex.getMessage(), ex);
            throw new MatchmakingException("Error occurred while fetching user: " + ex.getMessage(), UNKNOWN_EXCEPTION);
        }
    }

    private boolean isProfileComplete(User user) {
        return user.getFirstName() != null && !user.getFirstName().isBlank()
                && user.getLastName() != null && !user.getLastName().isBlank()
                && user.getGender() != null
                && user.getDateOfBirth() != null;
//                && user.getInterests() != null && !user.getInterests().isEmpty();
    }
}
