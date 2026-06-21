package com.shiviishiv7.matchmaking.processor.userprofile;

import com.shiviishiv7.matchmaking.common.exception.MatchmakingException;
import com.shiviishiv7.matchmaking.provider.implementation.MatrimonialExtProfileRepository;
import com.shiviishiv7.matchmaking.provider.model.MatrimonialExtProfile;

import com.shiviishiv7.matchmaking.provider.vo.BaseVO;
import com.shiviishiv7.matchmaking.provider.vo.MatrimonialExtProfileVO;

import jakarta.transaction.Transactional;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;

import java.util.Optional;

import static com.shiviishiv7.matchmaking.common.constants.MatchmakingHttpStatus.*;

@Component
@Transactional
@Slf4j
public class MatrimonialExtProfileProcessor implements IMatrimonialExtProfileProcessor {

    @Autowired
    private MatrimonialExtProfileRepository matrimonialExtProfileRepository;

    @Override
    public BaseVO add(MatrimonialExtProfileVO vo) throws MatchmakingException {
        try {
            log.info("Validating inputs for matrimonial profile creation. {}", vo.toString());
            vo.validate();
            log.info("MatrimonialExtProfileVO validation completed successfully.");

            log.trace("Checking for duplicate matrimonial profile for userId: {}", vo.getUserId());
            if (matrimonialExtProfileRepository.existsByUserId(vo.getUserId())) {
                log.error("ALERT_FOR_ERROR: matrimonial profile already exists for userId: {}", vo.getUserId());
                throw new MatchmakingException("matrimonial profile already exists for this user", DUPLICATE_RECORD);
            }

            log.trace("Saving matrimonial profile for userId: {}", vo.getUserId());
            MatrimonialExtProfile profile = new MatrimonialExtProfile();
            profile.fromVO(vo);
            profile = matrimonialExtProfileRepository.save(profile);
            log.info("matrimonial profile saved successfully for userId: {}", profile.getUserId());

            return new BaseVO(SUCCESS, "matrimonial profile created", "matrimonial profile created", profile.toVO());
        } catch (MatchmakingException ex) {
            throw ex;
        } catch (Exception ex) {
            log.error("ALERT_FOR_ERROR: Error occurred while adding matrimonial profile. Error: {}", ex.getMessage(), ex);
            throw new MatchmakingException("Error occurred while adding matrimonial profile: " + ex.getMessage(), UNKNOWN_EXCEPTION);
        }
    }

    @Override
    public BaseVO update(MatrimonialExtProfileVO vo) throws MatchmakingException {
        try {
            log.info("Validating inputs for matrimonial profile update.");
            if (vo.getId() == null) {
                throw new MatchmakingException("MatrimonialExtProfile ID cannot be null for update", VALIDATION_ERROR);
            }
            vo.validate();
            log.info("MatrimonialExtProfileVO validation completed successfully.");

            log.trace("Fetching existing matrimonial profile for ID: {}", vo.getId());
            Optional<MatrimonialExtProfile> fromDB = matrimonialExtProfileRepository.findById(vo.getId());
            if (fromDB.isEmpty()) {
                log.error("ALERT_FOR_ERROR: matrimonial profile not found for ID: {}", vo.getId());
                throw new MatchmakingException("matrimonial profile does not exist", DATA_NOT_FOUND);
            }

            MatrimonialExtProfile profile = fromDB.get();
            log.trace("matrimonial profile found for ID: {}. Applying updates.", vo.getId());

            profile.setReligion(vo.getReligion());
            profile.setCaste(vo.getCaste());
            profile.setSubCaste(vo.getSubCaste());
            profile.setGotram(vo.getGotram());
            profile.setMotherTongue(vo.getMotherTongue());
            profile.setDietaryHabits(vo.getDietaryHabits());
            profile.setHighestEducation(vo.getHighestEducation());
            profile.setEducationDetail(vo.getEducationDetail());
            profile.setProfession(vo.getProfession());
            profile.setEmployerName(vo.getEmployerName());
            profile.setEmploymentType(vo.getEmploymentType());
            profile.setAnnualIncomeInr(vo.getAnnualIncomeInr());
            profile.setNativeCity(vo.getNativeCity());
            profile.setNativeState(vo.getNativeState());
            profile.setFamilyType(vo.getFamilyType());
            profile.setFamilyValues(vo.getFamilyValues());
            profile.setFamilyStatus(vo.getFamilyStatus());
            profile.setFatherOccupation(vo.getFatherOccupation());
            profile.setMotherOccupation(vo.getMotherOccupation());
            profile.setSiblingsCount(vo.getSiblingsCount());
            profile.setSiblingsDetail(vo.getSiblingsDetail());
            profile.setHeightCm(vo.getHeightCm());
            profile.setMaritalStatus(vo.getMaritalStatus());
            profile.setBodyType(vo.getBodyType());
            profile.setComplexion(vo.getComplexion());
            profile.setSmokingHabit(vo.getSmokingHabit());
            profile.setDrinkingHabit(vo.getDrinkingHabit());
            profile.setManglikStatus(vo.getManglikStatus());
            profile.setRashi(vo.getRashi());
            profile.setNakshatra(vo.getNakshatra());
            profile.setBirthPlace(vo.getBirthPlace());
            profile.setBirthTime(vo.getBirthTime());
            profile.setHoroscopeMatchRequired(vo.getHoroscopeMatchRequired());

            profile = matrimonialExtProfileRepository.save(profile);
            log.info("matrimonial profile updated successfully for ID: {}", profile.getId());

            return new BaseVO(SUCCESS, "matrimonial profile updated", "matrimonial profile updated", profile.toVO());
        } catch (MatchmakingException ex) {
            throw ex;
        } catch (Exception ex) {
            log.error("ALERT_FOR_ERROR: Error occurred while updating matrimonial profile. Error: {}", ex.getMessage(), ex);
            throw new MatchmakingException("Error occurred while updating matrimonial profile: " + ex.getMessage(), UNKNOWN_EXCEPTION);
        }
    }

    @Override
    public BaseVO getById(String id) throws MatchmakingException {
        try {
            log.info("Fetching matrimonial profile for ID: {}", id);
            Optional<MatrimonialExtProfile> fromDB = matrimonialExtProfileRepository.findById(Integer.valueOf(id));
            if (fromDB.isEmpty()) {
                log.error("ALERT_FOR_ERROR: matrimonial profile not found for ID: {}", id);
                throw new MatchmakingException("matrimonial profile does not exist", DATA_NOT_FOUND);
            }
            log.info("matrimonial profile found for ID: {}", id);
            return new BaseVO(SUCCESS, "matrimonial profile fetched", "matrimonial profile fetched", fromDB.get().toVO());
        } catch (MatchmakingException ex) {
            throw ex;
        } catch (Exception ex) {
            log.error("ALERT_FOR_ERROR: Error occurred while fetching matrimonial profile. Error: {}", ex.getMessage(), ex);
            throw new MatchmakingException("Error occurred while fetching matrimonial profile: " + ex.getMessage(), UNKNOWN_EXCEPTION);
        }
    }

    @Override
    public BaseVO getByUserId(String userId) throws MatchmakingException {
        try {
            log.info("Fetching matrimonial profile for userId: {}", userId);
            Optional<MatrimonialExtProfile> fromDB = matrimonialExtProfileRepository.findByUserId(Integer.valueOf(userId));
            if (fromDB.isEmpty()) {
                log.error("ALERT_FOR_ERROR: matrimonial profile not found for userId: {}", userId);
                throw new MatchmakingException("matrimonial profile does not exist for this user", DATA_NOT_FOUND);
            }
            log.info("matrimonial profile found for userId: {}", userId);
            return new BaseVO(SUCCESS, "matrimonial profile fetched", "matrimonial profile fetched", fromDB.get().toVO());
        } catch (MatchmakingException ex) {
            throw ex;
        } catch (Exception ex) {
            log.error("ALERT_FOR_ERROR: Error occurred while fetching matrimonial profile by userId. Error: {}", ex.getMessage(), ex);
            throw new MatchmakingException("Error occurred while fetching matrimonial profile: " + ex.getMessage(), UNKNOWN_EXCEPTION);
        }
    }

    @Override
    public BaseVO delete(String id) throws MatchmakingException {
        try {
            log.info("Deleting matrimonial profile for ID: {}", id);
            Optional<MatrimonialExtProfile> fromDB = matrimonialExtProfileRepository.findById(Integer.valueOf(id));
            if (fromDB.isEmpty()) {
                log.error("ALERT_FOR_ERROR: matrimonial profile not found for ID: {}", id);
                throw new MatchmakingException("matrimonial profile does not exist", DATA_NOT_FOUND);
            }
            matrimonialExtProfileRepository.deleteById(Integer.valueOf(id));
            log.info("matrimonial profile hard-deleted for ID: {}", id);
            return new BaseVO(SUCCESS, "matrimonial profile deleted", "matrimonial profile deleted");
        } catch (MatchmakingException ex) {
            throw ex;
        } catch (Exception ex) {
            log.error("ALERT_FOR_ERROR: Error occurred while deleting matrimonial profile. Error: {}", ex.getMessage(), ex);
            throw new MatchmakingException("Error occurred while deleting matrimonial profile: " + ex.getMessage(), UNKNOWN_EXCEPTION);
        }
    }
}
