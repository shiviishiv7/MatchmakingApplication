package com.shiviishiv7.matchmaking.processor.user;

import com.shiviishiv7.matchmaking.common.enums.UserStatus;
import com.shiviishiv7.matchmaking.common.exception.MatchmakingException;
import com.shiviishiv7.matchmaking.provider.implementation.CompanyRepository;
import com.shiviishiv7.matchmaking.provider.implementation.UserRepository;
import com.shiviishiv7.matchmaking.provider.model.Company;
import com.shiviishiv7.matchmaking.provider.model.User;
import com.shiviishiv7.matchmaking.provider.vo.BaseVO;
import com.shiviishiv7.matchmaking.provider.vo.UserVO;
import jakarta.transaction.Transactional;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;

import java.util.Optional;
import java.util.UUID;

import static com.shiviishiv7.matchmaking.common.constants.MatchmakingHttpStatus.*;

@Component
@Transactional
@Slf4j
public class UserProcessor implements IUserProcessor {

    @Autowired
    private UserRepository userRepository;

    @Autowired
    private CompanyRepository companyRepository;

    @Override
    public BaseVO add(UserVO userVO) throws MatchmakingException {
        try {
            log.info("Validating inputs for user creation.{}", userVO.toString());
//            userVO.validate();
            log.info("UserVO validation completed successfully.");

            log.trace("Checking for duplicate email: {}", userVO.getEmail());
            if (userRepository.existsByEmail(userVO.getEmail())) {
                log.error("ALERT_FOR_ERROR: Duplicate email found: {}", userVO.getEmail());
                throw new MatchmakingException("User with email already exists", DUPLICATE_RECORD);
            }

            log.trace("Fetching company for ID: {}", userVO.getCompanyId());
            Optional<Company> optionalCompany = companyRepository.findById(userVO.getCompanyId());
            if (optionalCompany.isEmpty()) {
                log.error("ALERT_FOR_ERROR: Company not found for ID: {}", userVO.getCompanyId());
                throw new MatchmakingException("Company does not exist", DATA_NOT_FOUND);
            }

            log.trace("Saving user record for email: {}", userVO.getEmail());
            User user = userVO.fromVO();
//            user.setCompany(optionalCompany.get());
            user = userRepository.save(user);
            log.info("User record saved successfully for email: {}", user.getEmail());

            return new BaseVO(SUCCESS, "User record saved", "User record saved", new UserVO().toVO(user));
        } catch (MatchmakingException ex) {
            throw ex;
        } catch (Exception ex) {
            log.error("ALERT_FOR_ERROR: Error occurred while adding user. Error: {}", ex.getMessage(), ex);
            throw new MatchmakingException("Error occurred while adding user: " + ex.getMessage(), UNKNOWN_EXCEPTION);
        }
    }

    @Override
    public BaseVO update(UserVO userVO) throws MatchmakingException {
        try {
            log.info("Validating inputs for user update.");
            if (userVO.getId() == null) {
                throw new MatchmakingException("User ID cannot be null for update", VALIDATION_ERROR);
            }
            userVO.validate();
            log.info("UserVO validation completed successfully.");

            log.trace("Fetching existing user for ID: {}", userVO.getId());
            Optional<User> userFromDBOptional = userRepository.findById(userVO.getId());
            if (userFromDBOptional.isEmpty()) {
                log.error("ALERT_FOR_ERROR: User not found for ID: {}", userVO.getId());
                throw new MatchmakingException("User does not exist", DATA_NOT_FOUND);
            }

            User userFromDB = userFromDBOptional.get();
            log.trace("User found for ID: {}. Applying updates.", userVO.getId());

//            if (userVO.getCompanyId() != null && !userVO.getCompanyId().equals(
//                    userFromDB.getCompany() != null ? userFromDB.getCompany().getId() : null)) {
//                log.trace("Company change requested. Fetching new company for ID: {}", userVO.getCompanyId());
//                Optional<Company> optionalCompany = companyRepository.findById(userVO.getCompanyId());
//                if (optionalCompany.isEmpty()) {
//                    log.error("ALERT_FOR_ERROR: Company not found for ID: {}", userVO.getCompanyId());
//                    throw new MatchmakingException("Company does not exist", DATA_NOT_FOUND);
//                }
//                userFromDB.setCompany(optionalCompany.get());
//            }

            userFromDB.setFirstName(userVO.getFirstName());
            userFromDB.setLastName(userVO.getLastName());
            userFromDB.setGender(userVO.getGender());
            userFromDB.setDateOfBirth(userVO.getDateOfBirth());
            userFromDB.setTimezone(userVO.getTimezone());
            userFromDB.setIndustry(userVO.getIndustry());
            userFromDB.setBio(userVO.getBio());
            userFromDB.setProfilePictureUrl(userVO.getProfilePictureUrl());
//            userFromDB.setInterests(userVO.getInterests());
            if (userVO.getStatus() != null) {
                userFromDB.setStatus(userVO.getStatus());
            }

            // Auto-transition to IN_POOL once the profile is fully filled in
            if (isProfileComplete(userFromDB) &&
                    (userFromDB.getStatus() == UserStatus.PENDING_VERIFICATION ||
                     userFromDB.getStatus() == UserStatus.INCOMPLETE_PROFILE)) {
                log.info("Profile is complete for user ID: {}. Transitioning status to IN_POOL.", userFromDB.getId());
                userFromDB.setStatus(UserStatus.IN_POOL);
            } else if (!isProfileComplete(userFromDB) &&
                    userFromDB.getStatus() == UserStatus.PENDING_VERIFICATION) {
                userFromDB.setStatus(UserStatus.INCOMPLETE_PROFILE);
            }

            userFromDB = userRepository.save(userFromDB);
            log.info("User record updated successfully for ID: {}", userFromDB.getId());

            return new BaseVO(SUCCESS, "User record updated", "User record updated", new UserVO().toVO(userFromDB));
        } catch (MatchmakingException ex) {
            throw ex;
        } catch (Exception ex) {
            log.error("ALERT_FOR_ERROR: Error occurred while updating user. Error: {}", ex.getMessage(), ex);
            throw new MatchmakingException("Error occurred while updating user: " + ex.getMessage(), UNKNOWN_EXCEPTION);
        }
    }

    @Override
    public BaseVO get(UUID id) throws MatchmakingException {
        try {
            log.info("Fetching user for ID: {}", id);
            Optional<User> optionalUser = userRepository.findById(id);
            if (optionalUser.isEmpty()) {
                log.error("ALERT_FOR_ERROR: User not found for ID: {}", id);
                throw new MatchmakingException("User does not exist", DATA_NOT_FOUND);
            }

            log.info("User found for ID: {}", id);
            return new BaseVO(SUCCESS, "User record fetched", "User record fetched", new UserVO().toVO(optionalUser.get()));
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

            Optional<User> optionalUser = userRepository.findByEmail(email);
            if (optionalUser.isEmpty()) {
                log.error("ALERT_FOR_ERROR: User not found for email: {}", email);
                throw new MatchmakingException("User does not exist", DATA_NOT_FOUND);
            }

            log.info("User found for email: {}", email);
            return new BaseVO(SUCCESS, "User record fetched", "User record fetched", new UserVO().toVO(optionalUser.get()));
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
                && user.getDateOfBirth() != null
                && user.getTimezone() != null && !user.getTimezone().isBlank()
                && user.getIndustry() != null && !user.getIndustry().isBlank()
                && user.getBio() != null && !user.getBio().isBlank();
//                && user.getInterests() != null && !user.getInterests().isEmpty();
    }

    @Override
    public BaseVO delete(UUID id) throws MatchmakingException {
        try {
            log.info("Deactivating user for ID: {}", id);
            Optional<User> optionalUser = userRepository.findById(id);
            if (optionalUser.isEmpty()) {
                log.error("ALERT_FOR_ERROR: User not found for ID: {}", id);
                throw new MatchmakingException("User does not exist", DATA_NOT_FOUND);
            }

            User user = optionalUser.get();
            user.setIsActive(false);
            userRepository.save(user);
            log.info("User soft-deleted (deactivated) for ID: {}", id);

            return new BaseVO(SUCCESS, "User deactivated", "User record deactivated");
        } catch (MatchmakingException ex) {
            throw ex;
        } catch (Exception ex) {
            log.error("ALERT_FOR_ERROR: Error occurred while deleting user. Error: {}", ex.getMessage(), ex);
            throw new MatchmakingException("Error occurred while deleting user: " + ex.getMessage(), UNKNOWN_EXCEPTION);
        }
    }
}
