package com.shiviishiv7.matchmaking.processor.userprofile;

import com.shiviishiv7.matchmaking.common.exception.MatchmakingException;
import com.shiviishiv7.matchmaking.provider.implementation.PartnerPreferenceRepository;
import com.shiviishiv7.matchmaking.provider.model.PartnerPreference;
import com.shiviishiv7.matchmaking.provider.vo.BaseVO;
import com.shiviishiv7.matchmaking.provider.vo.PartnerPreferenceVO;
import jakarta.transaction.Transactional;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Component;

import java.util.Optional;

import static com.shiviishiv7.matchmaking.common.constants.MatchmakingHttpStatus.*;

@Component
@Transactional
@RequiredArgsConstructor
@Slf4j
public class PartnerPreferenceProcessor implements IPartnerPreferenceProcessor {

    private final PartnerPreferenceRepository partnerPreferenceRepository;

    @Override
    public BaseVO add(PartnerPreferenceVO vo) throws MatchmakingException {
        try {
            vo.validate();
            PartnerPreference profile = new PartnerPreference();
            profile.fromVO(vo);
            profile = partnerPreferenceRepository.save(profile);
            log.info("Partner preference saved for cognitoSub={} postId={}", vo.getCognitoSub(), vo.getPostId());
            return new BaseVO(SUCCESS, "Partner preference created", "Partner preference created", profile.toVO());
        } catch (MatchmakingException ex) {
            throw ex;
        } catch (Exception ex) {
            log.error("ALERT_FOR_ERROR: Error adding partner preference: {}", ex.getMessage(), ex);
            throw new MatchmakingException("Error adding partner preference: " + ex.getMessage(), UNKNOWN_EXCEPTION);
        }
    }

    @Override
    public BaseVO update(PartnerPreferenceVO vo) throws MatchmakingException {
        try {
            if (vo.getCognitoSub() == null) {
                throw new MatchmakingException("cognitoSub cannot be null for update", VALIDATION_ERROR);
            }
            vo.validate();

            Optional<PartnerPreference> fromDB = vo.getPostId() != null
                    ? partnerPreferenceRepository.findByPostId(vo.getPostId())
                    : partnerPreferenceRepository.findByCognitoSub(vo.getCognitoSub());

            if (fromDB.isEmpty()) {
                throw new MatchmakingException("Partner preference does not exist", DATA_NOT_FOUND);
            }

            PartnerPreference profile = fromDB.get();
            profile.setAgeMin(vo.getAgeMin());
            profile.setAgeMax(vo.getAgeMax());
            profile.setHeightMinCm(vo.getHeightMinCm());
            profile.setHeightMaxCm(vo.getHeightMaxCm());
            profile.setGenderPref(vo.getGenderPref());
            profile.setMaritalStatusPref(vo.getMaritalStatusPref());
            profile.setPreferredStates(vo.getPreferredStates());
            profile.setOpenToRelocation(vo.getOpenToRelocation());
            profile.setReligionPref(vo.getReligionPref());
            profile.setMotherTonguePref(vo.getMotherTonguePref());
            profile.setDietaryPref(vo.getDietaryPref());
            profile.setEducationPref(vo.getEducationPref());
            profile.setEmploymentTypePref(vo.getEmploymentTypePref());
            profile.setIncomeMinInr(vo.getIncomeMinInr());
            profile.setIncomeMaxInr(vo.getIncomeMaxInr());
            profile.setSmokingPref(vo.getSmokingPref());
            profile.setDrinkingPref(vo.getDrinkingPref());
            profile.setFamilyTypePref(vo.getFamilyTypePref());
            profile.setFamilyValuesPref(vo.getFamilyValuesPref());
            profile.setWantsChildrenPref(vo.getWantsChildrenPref());
            profile.setMarriageTimelinePref(vo.getMarriageTimelinePref());
            profile.setOkWithPartnerWorkingPref(vo.getOkWithPartnerWorkingPref());
            profile.setRelationshipGoalPref(vo.getRelationshipGoalPref());
            profile.setAboutPartner(vo.getAboutPartner());
            profile = partnerPreferenceRepository.save(profile);
            log.info("Partner preference updated for id={}", profile.getId());
            return new BaseVO(SUCCESS, "Partner preference updated", "Partner preference updated", profile.toVO());
        } catch (MatchmakingException ex) {
            throw ex;
        } catch (Exception ex) {
            log.error("ALERT_FOR_ERROR: Error updating partner preference: {}", ex.getMessage(), ex);
            throw new MatchmakingException("Error updating partner preference: " + ex.getMessage(), UNKNOWN_EXCEPTION);
        }
    }

    @Override
    public BaseVO getByCognitoSub(String cognitoSub) throws MatchmakingException {
        try {
            Optional<PartnerPreference> fromDB = partnerPreferenceRepository.findByCognitoSub(cognitoSub);
            if (fromDB.isEmpty()) {
                throw new MatchmakingException("Partner preference does not exist", DATA_NOT_FOUND);
            }
            return new BaseVO(SUCCESS, "Partner preference fetched", "Partner preference fetched", fromDB.get().toVO());
        } catch (MatchmakingException ex) {
            throw ex;
        } catch (Exception ex) {
            log.error("ALERT_FOR_ERROR: Error fetching partner preference: {}", ex.getMessage(), ex);
            throw new MatchmakingException("Error fetching partner preference: " + ex.getMessage(), UNKNOWN_EXCEPTION);
        }
    }

    @Override
    public BaseVO getByUserId(String userId) throws MatchmakingException {
        return getByCognitoSub(userId);
    }

    @Override
    public BaseVO delete(String cognitoSub) throws MatchmakingException {
        try {
            Optional<PartnerPreference> fromDB = partnerPreferenceRepository.findByCognitoSub(cognitoSub);
            if (fromDB.isEmpty()) {
                throw new MatchmakingException("Partner preference does not exist", DATA_NOT_FOUND);
            }
            partnerPreferenceRepository.delete(fromDB.get());
            log.info("Partner preference deleted for cognitoSub={}", cognitoSub);
            return new BaseVO(SUCCESS, "Partner preference deleted", "Partner preference deleted");
        } catch (MatchmakingException ex) {
            throw ex;
        } catch (Exception ex) {
            log.error("ALERT_FOR_ERROR: Error deleting partner preference: {}", ex.getMessage(), ex);
            throw new MatchmakingException("Error deleting partner preference: " + ex.getMessage(), UNKNOWN_EXCEPTION);
        }
    }
}
