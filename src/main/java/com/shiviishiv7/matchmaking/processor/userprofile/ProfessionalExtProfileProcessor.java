package com.shiviishiv7.matchmaking.processor.userprofile;

import com.shiviishiv7.matchmaking.common.exception.MatchmakingException;
import com.shiviishiv7.matchmaking.provider.implementation.ProfessionalExtProfileRepository;
import com.shiviishiv7.matchmaking.provider.model.profile.ProfessionalExtProfile;

import com.shiviishiv7.matchmaking.provider.vo.BaseVO;
import com.shiviishiv7.matchmaking.provider.vo.MatchFilterVO;
import com.shiviishiv7.matchmaking.provider.vo.ProfessionalExtProfileVO;

import jakarta.transaction.Transactional;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;

import java.util.Optional;

import static com.shiviishiv7.matchmaking.common.constants.MatchmakingHttpStatus.*;

@Component
@Transactional
@Slf4j
public class ProfessionalExtProfileProcessor implements IProfessionalExtProfileProcessor {

    @Autowired
    private ProfessionalExtProfileRepository professionalExtProfileRepository;

    @Override
    public BaseVO add(ProfessionalExtProfileVO vo) throws MatchmakingException {
        try {
            log.info("Validating inputs for professional profile creation. {}", vo.toString());
            vo.validate();
            log.info("ProfessionalExtProfileVO validation completed successfully.");

            log.trace("Checking for duplicate professional profile for userId: {}", vo.getCognitoSub());
            if (professionalExtProfileRepository.existsByCognitoSub(vo.getCognitoSub())) {
                log.error("ALERT_FOR_ERROR: professional profile already exists for userId: {}", vo.getCognitoSub());
                throw new MatchmakingException("professional profile already exists for this user", DUPLICATE_RECORD);
            }

            log.trace("Saving professional profile for userId: {}", vo.getCognitoSub());
            ProfessionalExtProfile profile = new ProfessionalExtProfile();
            profile.fromVO(vo);
            profile = professionalExtProfileRepository.save(profile);
            log.info("professional profile saved successfully for userId: {}", profile.getCognitoSub());

            return new BaseVO(SUCCESS, "professional profile created", "professional profile created", profile.toVO());
        } catch (MatchmakingException ex) {
            throw ex;
        } catch (Exception ex) {
            log.error("ALERT_FOR_ERROR: Error occurred while adding professional profile. Error: {}", ex.getMessage(), ex);
            throw new MatchmakingException("Error occurred while adding professional profile: " + ex.getMessage(), UNKNOWN_EXCEPTION);
        }
    }

    @Override
    public BaseVO update(ProfessionalExtProfileVO vo) throws MatchmakingException {
        try {
            log.info("Validating inputs for professional profile update.");
            if (vo.getId() == null) {
                throw new MatchmakingException("ProfessionalExtProfile ID cannot be null for update", VALIDATION_ERROR);
            }
            vo.validate();
            log.info("ProfessionalExtProfileVO validation completed successfully.");

            log.trace("Fetching existing professional profile for ID: {}", vo.getId());
            Optional<ProfessionalExtProfile> fromDB = professionalExtProfileRepository.findById(vo.getId());
            if (fromDB.isEmpty()) {
                log.error("ALERT_FOR_ERROR: professional profile not found for ID: {}", vo.getId());
                throw new MatchmakingException("professional profile does not exist", DATA_NOT_FOUND);
            }

            ProfessionalExtProfile profile = fromDB.get();
            log.trace("professional profile found for ID: {}. Applying updates.", vo.getId());

            profile.setCurrentRole(vo.getCurrentRole());
            profile.setCurrentCompany(vo.getCurrentCompany());
            profile.setYearsOfExperience(vo.getYearsOfExperience());
            profile.setIndustryDomain(vo.getIndustryDomain());
            profile.setTechStack(vo.getTechStack());
            profile.setSkillsOffering(vo.getSkillsOffering());
            profile.setSkillsSeeking(vo.getSkillsSeeking());
            profile.setMentorshipRole(vo.getMentorshipRole());
            profile.setOpenToCoFounder(vo.getOpenToCoFounder());
            profile.setStartupIdeas(vo.getStartupIdeas());
            profile.setLinkedinUrl(vo.getLinkedinUrl());
            profile.setGithubUrl(vo.getGithubUrl());
            profile.setPortfolioUrl(vo.getPortfolioUrl());
            profile.setCertifications(vo.getCertifications());
            profile.setPreferredCollabMode(vo.getPreferredCollabMode());
            profile.setAvailabilitySlots(vo.getAvailabilitySlots());

            profile = professionalExtProfileRepository.save(profile);
            log.info("professional profile updated successfully for ID: {}", profile.getId());

            return new BaseVO(SUCCESS, "professional profile updated", "professional profile updated", profile.toVO());
        } catch (MatchmakingException ex) {
            throw ex;
        } catch (Exception ex) {
            log.error("ALERT_FOR_ERROR: Error occurred while updating professional profile. Error: {}", ex.getMessage(), ex);
            throw new MatchmakingException("Error occurred while updating professional profile: " + ex.getMessage(), UNKNOWN_EXCEPTION);
        }
    }

    @Override
    public BaseVO getById(String id) throws MatchmakingException {
        try {
            log.info("Fetching professional profile for ID: {}", id);
            Optional<ProfessionalExtProfile> fromDB = professionalExtProfileRepository.findById(Integer.valueOf(id));
            if (fromDB.isEmpty()) {
                log.error("ALERT_FOR_ERROR: professional profile not found for ID: {}", id);
                throw new MatchmakingException("professional profile does not exist", DATA_NOT_FOUND);
            }
            log.info("professional profile found for ID: {}", id);
            return new BaseVO(SUCCESS, "professional profile fetched", "professional profile fetched", fromDB.get().toVO());
        } catch (MatchmakingException ex) {
            throw ex;
        } catch (Exception ex) {
            log.error("ALERT_FOR_ERROR: Error occurred while fetching professional profile. Error: {}", ex.getMessage(), ex);
            throw new MatchmakingException("Error occurred while fetching professional profile: " + ex.getMessage(), UNKNOWN_EXCEPTION);
        }
    }

    @Override
    public BaseVO getByUserId(String userId) throws MatchmakingException {
        try {
            log.info("Fetching professional profile for userId: {}", userId);
            Optional<ProfessionalExtProfile> fromDB = professionalExtProfileRepository.findByCognitoSub((userId));
            if (fromDB.isEmpty()) {
                log.error("ALERT_FOR_ERROR: professional profile not found for userId: {}", userId);
                throw new MatchmakingException("professional profile does not exist for this user", DATA_NOT_FOUND);
            }
            log.info("professional profile found for userId: {}", userId);
            return new BaseVO(SUCCESS, "professional profile fetched", "professional profile fetched", fromDB.get().toVO());
        } catch (MatchmakingException ex) {
            throw ex;
        } catch (Exception ex) {
            log.error("ALERT_FOR_ERROR: Error occurred while fetching professional profile by userId. Error: {}", ex.getMessage(), ex);
            throw new MatchmakingException("Error occurred while fetching professional profile: " + ex.getMessage(), UNKNOWN_EXCEPTION);
        }
    }

    @Override
    public BaseVO delete(String id) throws MatchmakingException {
        try {
            log.info("Deleting professional profile for ID: {}", id);
            Optional<ProfessionalExtProfile> fromDB = professionalExtProfileRepository.findById(Integer.valueOf(id));
            if (fromDB.isEmpty()) {
                log.error("ALERT_FOR_ERROR: professional profile not found for ID: {}", id);
                throw new MatchmakingException("professional profile does not exist", DATA_NOT_FOUND);
            }
            professionalExtProfileRepository.deleteById(Integer.valueOf(id));
            log.info("professional profile hard-deleted for ID: {}", id);
            return new BaseVO(SUCCESS, "professional profile deleted", "professional profile deleted");
        } catch (MatchmakingException ex) {
            throw ex;
        } catch (Exception ex) {
            log.error("ALERT_FOR_ERROR: Error occurred while deleting professional profile. Error: {}", ex.getMessage(), ex);
            throw new MatchmakingException("Error occurred while deleting professional profile: " + ex.getMessage(), UNKNOWN_EXCEPTION);
        }
    }

    @Override
    public void upsertFromFilter(MatchFilterVO vo) throws MatchmakingException {
        try {
            ProfessionalExtProfile profile = professionalExtProfileRepository
                    .findByCognitoSub(vo.getCognitoSub())
                    .orElse(new ProfessionalExtProfile());
            profile.setCognitoSub(vo.getCognitoSub());
            profile.setCurrentRole(vo.getCurrentRole());
            profile.setCurrentCompany(vo.getCurrentCompany());
            profile.setYearsOfExperience(vo.getYearsOfExperience());
            profile.setIndustryDomain(vo.getIndustryDomain());
            profile.setTechStack(vo.getTechStack());
            profile.setSkillsOffering(vo.getSkillsOffering());
            profile.setSkillsSeeking(vo.getSkillsSeeking());
            profile.setMentorshipRole(vo.getMentorshipRole());
            profile.setOpenToCoFounder(vo.getOpenToCoFounder());
            profile.setPreferredCollabMode(vo.getPreferredCollabMode());
            professionalExtProfileRepository.save(profile);
            log.info("Professional ext profile upserted for cognitoSub: {}", vo.getCognitoSub());
        } catch (Exception ex) {
            log.error("ALERT_FOR_ERROR: Error upserting professional profile. Error: {}", ex.getMessage(), ex);
            throw new MatchmakingException("Error upserting professional profile: " + ex.getMessage(), UNKNOWN_EXCEPTION);
        }
    }
}