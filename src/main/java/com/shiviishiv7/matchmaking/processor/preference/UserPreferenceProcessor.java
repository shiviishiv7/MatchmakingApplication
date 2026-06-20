package com.shiviishiv7.matchmaking.processor.preference;

import com.shiviishiv7.matchmaking.common.exception.MatchmakingException;
import com.shiviishiv7.matchmaking.provider.implementation.UserPreferenceRepository;
import com.shiviishiv7.matchmaking.provider.implementation.UserRepository;
import com.shiviishiv7.matchmaking.provider.model.User;
import com.shiviishiv7.matchmaking.provider.model.UserPreference;
import com.shiviishiv7.matchmaking.provider.vo.BaseVO;
import com.shiviishiv7.matchmaking.provider.vo.UserPreferenceVO;
import jakarta.transaction.Transactional;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;

import java.util.Optional;


import static com.shiviishiv7.matchmaking.common.constants.MatchmakingHttpStatus.*;

@Component
@Transactional
@Slf4j
public class UserPreferenceProcessor implements IUserPreferenceProcessor {

    @Autowired
    private UserPreferenceRepository userPreferenceRepository;

    @Autowired
    private UserRepository userRepository;

    @Override
    public BaseVO add(UserPreferenceVO preferenceVO) throws MatchmakingException {
        try {
            log.info("Validating inputs for user preference creation.");
            preferenceVO.validate();
            log.info("UserPreferenceVO validation completed successfully.");

            log.trace("Fetching user for ID: {}", preferenceVO.getCognitoSub());
            Optional<User> optionalUser = userRepository.findByCognitoSub(preferenceVO.getCognitoSub());
            if (optionalUser.isEmpty()) {
                log.error("ALERT_FOR_ERROR: User not found for ID: {}", preferenceVO.getCognitoSub());
                throw new MatchmakingException("User does not exist", DATA_NOT_FOUND);
            }

            log.trace("Checking for existing preference for user ID: {}", preferenceVO.getCognitoSub());
            if (userPreferenceRepository.existsByCognitoSub(preferenceVO.getCognitoSub())) {
                log.error("ALERT_FOR_ERROR: Preference already exists for user ID: {}", preferenceVO.getCognitoSub());
                throw new MatchmakingException("Preference already exists for this user. Use update instead.", DUPLICATE_RECORD);
            }

            log.trace("Saving user preference for user ID: {}", preferenceVO.getCognitoSub());
            UserPreference preference = preferenceVO.fromVO();
            preference = userPreferenceRepository.save(preference);
            log.info("User preference saved successfully for user ID: {}", preferenceVO.getCognitoSub());

            return new BaseVO(SUCCESS, "User preference saved", "User preference record saved", new UserPreferenceVO().toVO(preference));
        } catch (MatchmakingException ex) {
            throw ex;
        } catch (Exception ex) {
            log.error("ALERT_FOR_ERROR: Error occurred while adding user preference. Error: {}", ex.getMessage(), ex);
            throw new MatchmakingException("Error occurred while adding user preference: " + ex.getMessage(), UNKNOWN_EXCEPTION);
        }
    }

    @Override
    public BaseVO update(UserPreferenceVO preferenceVO) throws MatchmakingException {
        try {
            log.info("Validating inputs for user preference update.");
            if (preferenceVO.getId() == null) {
                throw new MatchmakingException("Preference ID cannot be null for update", VALIDATION_ERROR);
            }
            preferenceVO.validate();
            log.info("UserPreferenceVO validation completed successfully.");

            log.trace("Fetching existing preference for ID: {}", preferenceVO.getId());
            Optional<UserPreference> preferenceFromDBOptional = userPreferenceRepository.findById(preferenceVO.getId());
            if (preferenceFromDBOptional.isEmpty()) {
                log.error("ALERT_FOR_ERROR: Preference not found for ID: {}", preferenceVO.getId());
                throw new MatchmakingException("User preference does not exist", DATA_NOT_FOUND);
            }

            UserPreference preferenceFromDB = preferenceFromDBOptional.get();
            log.trace("Preference found for ID: {}. Applying updates.", preferenceVO.getId());

            preferenceFromDB.setMinAge(preferenceVO.getMinAge());
            preferenceFromDB.setMaxAge(preferenceVO.getMaxAge());
            preferenceFromDB.setPreferredGender(preferenceVO.getPreferredGender());
//            preferenceFromDB.setPreferredIndustries(preferenceVO.getPreferredIndustries());
//            preferenceFromDB.setMaxTimezoneOffsetHours(preferenceVO.getMaxTimezoneOffsetHours());
            if (preferenceVO.getSameCompanyAllowed() != null) {
                preferenceFromDB.setSameCompanyAllowed(preferenceVO.getSameCompanyAllowed());
            }

            preferenceFromDB = userPreferenceRepository.save(preferenceFromDB);
            log.info("User preference updated successfully for ID: {}", preferenceFromDB.getId());

            return new BaseVO(SUCCESS, "User preference updated", "User preference record updated", new UserPreferenceVO().toVO(preferenceFromDB));
        } catch (MatchmakingException ex) {
            throw ex;
        } catch (Exception ex) {
            log.error("ALERT_FOR_ERROR: Error occurred while updating user preference. Error: {}", ex.getMessage(), ex);
            throw new MatchmakingException("Error occurred while updating user preference: " + ex.getMessage(), UNKNOWN_EXCEPTION);
        }
    }

    @Override
    public BaseVO getByUserId(String userId) throws MatchmakingException {
        try {
            log.info("Fetching user preference for user ID: {}", userId);
            Optional<UserPreference> optionalPreference = userPreferenceRepository.findByUserId(userId);
            if (optionalPreference.isEmpty()) {
                log.error("ALERT_FOR_ERROR: Preference not found for user ID: {}", userId);
                throw new MatchmakingException("User preference does not exist", DATA_NOT_FOUND);
            }

            log.info("User preference found for user ID: {}", userId);
            return new BaseVO(SUCCESS, "User preference fetched", "User preference record fetched", new UserPreferenceVO().toVO(optionalPreference.get()));
        } catch (MatchmakingException ex) {
            throw ex;
        } catch (Exception ex) {
            log.error("ALERT_FOR_ERROR: Error occurred while fetching user preference. Error: {}", ex.getMessage(), ex);
            throw new MatchmakingException("Error occurred while fetching user preference: " + ex.getMessage(), UNKNOWN_EXCEPTION);
        }
    }
}
