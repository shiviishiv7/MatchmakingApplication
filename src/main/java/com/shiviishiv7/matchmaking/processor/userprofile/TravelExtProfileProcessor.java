package com.shiviishiv7.matchmaking.processor.userprofile;

import com.shiviishiv7.matchmaking.common.exception.MatchmakingException;

import com.shiviishiv7.matchmaking.provider.implementation.TravelExtProfileRepository;
import com.shiviishiv7.matchmaking.provider.model.profile.TravelExtProfile;
import com.shiviishiv7.matchmaking.provider.vo.BaseVO;
import com.shiviishiv7.matchmaking.provider.vo.MatchFilterVO;
import com.shiviishiv7.matchmaking.provider.vo.TravelExtProfileVO;

import jakarta.transaction.Transactional;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;

import java.util.Optional;

import static com.shiviishiv7.matchmaking.common.constants.MatchmakingHttpStatus.*;

@Component
@Transactional
@Slf4j
public class TravelExtProfileProcessor implements ITravelExtProfileProcessor {

    @Autowired
    private TravelExtProfileRepository travelExtProfileRepository;

    @Override
    public BaseVO add(TravelExtProfileVO vo) throws MatchmakingException {
        try {
            log.info("Validating inputs for travel profile creation. {}", vo.toString());
            vo.validate();
            log.info("TravelExtProfileVO validation completed successfully.");

            log.trace("Checking for duplicate travel profile for userId: {}", vo.getCognitoSub());
            if (travelExtProfileRepository.existsByCognitoSub(vo.getCognitoSub())) {
                log.error("ALERT_FOR_ERROR: travel profile already exists for userId: {}", vo.getCognitoSub());
                throw new MatchmakingException("travel profile already exists for this user", DUPLICATE_RECORD);
            }

            log.trace("Saving travel profile for userId: {}", vo.getCognitoSub());
            TravelExtProfile profile = new TravelExtProfile();
            profile.fromVO(vo);
            profile = travelExtProfileRepository.save(profile);
            log.info("travel profile saved successfully for userId: {}", profile.getCognitoSub());

            return new BaseVO(SUCCESS, "travel profile created", "travel profile created", profile.toVO());
        } catch (MatchmakingException ex) {
            throw ex;
        } catch (Exception ex) {
            log.error("ALERT_FOR_ERROR: Error occurred while adding travel profile. Error: {}", ex.getMessage(), ex);
            throw new MatchmakingException("Error occurred while adding travel profile: " + ex.getMessage(), UNKNOWN_EXCEPTION);
        }
    }

    @Override
    public BaseVO update(TravelExtProfileVO vo) throws MatchmakingException {
        try {
            log.info("Validating inputs for travel profile update.");
            if (vo.getCognitoSub() == null) {
                throw new MatchmakingException("cognitoSub cannot be null for update", VALIDATION_ERROR);
            }
            vo.validate();
            log.info("TravelExtProfileVO validation completed successfully.");

            log.trace("Fetching profile for cognitoSub: {}", vo.getId());
            Optional<TravelExtProfile> fromDB = travelExtProfileRepository.findByCognitoSub(vo.getCognitoSub());
            if (fromDB.isEmpty()) {
                log.error("ALERT_FOR_ERROR: travel profile not found for cognitoSub: {}", vo.getId());
                throw new MatchmakingException("travel profile does not exist", DATA_NOT_FOUND);
            }

            TravelExtProfile profile = fromDB.get();
            log.trace("travel profile found for cognitoSub: {}. Applying updates.", vo.getId());

            profile.setTravelStyle(vo.getTravelStyle());
            profile.setPreferredDestinations(vo.getPreferredDestinations());
            profile.setBucketListPlaces(vo.getBucketListPlaces());
            profile.setTripsPerYear(vo.getTripsPerYear());
            profile.setPreferredTripDuration(vo.getPreferredTripDuration());
            profile.setHasTraveledAbroad(vo.getHasTraveledAbroad());
            profile.setCountriesVisited(vo.getCountriesVisited());
            profile.setDietaryNeeds(vo.getDietaryNeeds());
            profile.setIsOkWithBudgetStays(vo.getIsOkWithBudgetStays());
            profile.setIsOkWithCamping(vo.getIsOkWithCamping());
            profile.setPreferredGroupSize(vo.getPreferredGroupSize());
            profile.setUpcomingTrips(vo.getUpcomingTrips());
            profile.setPastTripsHighlights(vo.getPastTripsHighlights());

            profile = travelExtProfileRepository.save(profile);
            log.info("travel profile updated successfully for ID: {}", profile.getId());

            return new BaseVO(SUCCESS, "travel profile updated", "travel profile updated", profile.toVO());
        } catch (MatchmakingException ex) {
            throw ex;
        } catch (Exception ex) {
            log.error("ALERT_FOR_ERROR: Error occurred while updating travel profile. Error: {}", ex.getMessage(), ex);
            throw new MatchmakingException("Error occurred while updating travel profile: " + ex.getMessage(), UNKNOWN_EXCEPTION);
        }
    }

    @Override
    public BaseVO getByCognitoSub(String cognitoSub) throws MatchmakingException {
        try {
            log.info("Fetching profile for cognitoSub: {}", cognitoSub);
            Optional<TravelExtProfile> fromDB = travelExtProfileRepository.findByCognitoSub(cognitoSub);
            if (fromDB.isEmpty()) {
                log.error("ALERT_FOR_ERROR: profile not found for cognitoSub: {}", cognitoSub);
                throw new MatchmakingException("travel profile does not exist", DATA_NOT_FOUND);
            }
            log.info("Profile found for cognitoSub: {}", cognitoSub);
            return new BaseVO(SUCCESS, "travel profile fetched", "travel profile fetched", fromDB.get().toVO());
        } catch (MatchmakingException ex) {
            throw ex;
        } catch (Exception ex) {
            log.error("ALERT_FOR_ERROR: Error occurred while fetching travel profile. Error: {}", ex.getMessage(), ex);
            throw new MatchmakingException("Error occurred while fetching travel profile: " + ex.getMessage(), UNKNOWN_EXCEPTION);
        }
    }

    @Override
    public BaseVO getByUserId(String userId) throws MatchmakingException {
        try {
            log.info("Fetching travel profile for userId: {}", userId);
            Optional<TravelExtProfile> fromDB = travelExtProfileRepository.findByCognitoSub(userId);
            if (fromDB.isEmpty()) {
                log.error("ALERT_FOR_ERROR: travel profile not found for userId: {}", userId);
                throw new MatchmakingException("travel profile does not exist for this user", DATA_NOT_FOUND);
            }
            log.info("travel profile found for userId: {}", userId);
            return new BaseVO(SUCCESS, "travel profile fetched", "travel profile fetched", fromDB.get().toVO());
        } catch (MatchmakingException ex) {
            throw ex;
        } catch (Exception ex) {
            log.error("ALERT_FOR_ERROR: Error occurred while fetching travel profile by userId. Error: {}", ex.getMessage(), ex);
            throw new MatchmakingException("Error occurred while fetching travel profile: " + ex.getMessage(), UNKNOWN_EXCEPTION);
        }
    }

    @Override
    public BaseVO delete(String cognitoSub) throws MatchmakingException {
        try {
            log.info("Deleting travel profile for cognitoSub: {}", cognitoSub);
            Optional<TravelExtProfile> fromDB = travelExtProfileRepository.findByCognitoSub(cognitoSub);
            if (fromDB.isEmpty()) {
                log.error("ALERT_FOR_ERROR: profile not found for cognitoSub: {}", cognitoSub);
                throw new MatchmakingException("travel profile does not exist", DATA_NOT_FOUND);
            }
            travelExtProfileRepository.delete(fromDB.get());
            log.info("travel profile hard-deleted for cognitoSub: {}", cognitoSub);
            return new BaseVO(SUCCESS, "travel profile deleted", "travel profile deleted");
        } catch (MatchmakingException ex) {
            throw ex;
        } catch (Exception ex) {
            log.error("ALERT_FOR_ERROR: Error occurred while deleting travel profile. Error: {}", ex.getMessage(), ex);
            throw new MatchmakingException("Error occurred while deleting travel profile: " + ex.getMessage(), UNKNOWN_EXCEPTION);
        }
    }

    @Override
    public void upsertFromFilter(MatchFilterVO vo) throws MatchmakingException {
        try {
            TravelExtProfile profile = travelExtProfileRepository
                    .findByCognitoSub(vo.getCognitoSub())
                    .orElse(new TravelExtProfile());
            profile.setCognitoSub(vo.getCognitoSub());
            profile.setTravelStyle(vo.getTravelStyle());
            profile.setPreferredDestinations(vo.getPreferredDestinations());
            profile.setTripsPerYear(vo.getTripsPerYear());
            profile.setPreferredTripDuration(vo.getPreferredTripDuration());
            profile.setHasTraveledAbroad(vo.getHasTraveledAbroad());
            profile.setCountriesVisited(vo.getCountriesVisited());
            profile.setDietaryNeeds(vo.getDietaryNeeds());
            profile.setIsOkWithBudgetStays(vo.getIsOkWithBudgetStays());
            profile.setIsOkWithCamping(vo.getIsOkWithCamping());
            profile.setPreferredGroupSize(vo.getPreferredGroupSize());
            travelExtProfileRepository.save(profile);
            log.info("Travel ext profile upserted for cognitoSub: {}", vo.getCognitoSub());
        } catch (Exception ex) {
            log.error("ALERT_FOR_ERROR: Error upserting travel profile. Error: {}", ex.getMessage(), ex);
            throw new MatchmakingException("Error upserting travel profile: " + ex.getMessage(), UNKNOWN_EXCEPTION);
        }
    }
}