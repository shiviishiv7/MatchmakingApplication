package com.shiviishiv7.matchmaking.processor.preference;

import com.shiviishiv7.matchmaking.common.enums.MatchCategory;
import com.shiviishiv7.matchmaking.common.exception.MatchmakingException;
import com.shiviishiv7.matchmaking.processor.userprofile.*;
import com.shiviishiv7.matchmaking.provider.implementation.BaseUserProfileRepository;
import com.shiviishiv7.matchmaking.provider.implementation.CategoryProfileRegistryRepository;
import com.shiviishiv7.matchmaking.provider.model.CategoryProfileRegistry;
import com.shiviishiv7.matchmaking.provider.vo.BaseVO;
import com.shiviishiv7.matchmaking.provider.vo.MatchFilterVO;
import jakarta.transaction.Transactional;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;

import java.time.LocalDateTime;
import java.util.List;

import static com.shiviishiv7.matchmaking.common.constants.MatchmakingHttpStatus.*;

@Component
@Transactional
@Slf4j
public class UserPreferenceProcessor implements IUserPreferenceProcessor {

    @Autowired private BaseUserProfileRepository baseUserProfileRepository;
    @Autowired private CategoryProfileRegistryRepository registryRepository;
    @Autowired private IMatrimonialExtProfileProcessor matrimonialProcessor;
    @Autowired private IDatingExtProfileProcessor datingProcessor;
    @Autowired private IFitnessExtProfileProcessor fitnessProcessor;
    @Autowired private IFlatmateExtProfileProcessor flatmateProcessor;
    @Autowired private IGamingExtProfileProcessor gamingProcessor;
    @Autowired private IProfessionalExtProfileProcessor professionalProcessor;
    @Autowired private ITravelExtProfileProcessor travelProcessor;

    @Override
    public BaseVO save(MatchFilterVO vo) throws MatchmakingException {
        try {
            vo.validate();

            if (!baseUserProfileRepository.existsByCognitoSub(vo.getCognitoSub())) {
                log.error("ALERT_FOR_ERROR: User not found for cognitoSub: {}", vo.getCognitoSub());
                throw new MatchmakingException("User does not exist", DATA_NOT_FOUND);
            }

            MatchCategory category = MatchCategory.valueOf(vo.getChildCategory().trim().toUpperCase());
            upsertRegistry(vo, category);
            routeToExtProcessor(vo, category);

            log.info("Match filter saved for cognitoSub: {} category: {}", vo.getCognitoSub(), category);
            return new BaseVO(SUCCESS, "Match filter saved", "Match filter saved");
        } catch (MatchmakingException ex) {
            throw ex;
        } catch (Exception ex) {
            log.error("ALERT_FOR_ERROR: Error saving match filter. Error: {}", ex.getMessage(), ex);
            throw new MatchmakingException("Error saving match filter: " + ex.getMessage(), UNKNOWN_EXCEPTION);
        }
    }

    @Override
    public BaseVO getByUserId(String cognitoSub) throws MatchmakingException {
        try {
            List<CategoryProfileRegistry> entries = registryRepository.findByCognitoSub(cognitoSub);
            if (entries.isEmpty()) {
                throw new MatchmakingException("No match filters found for user", DATA_NOT_FOUND);
            }
            return new BaseVO(SUCCESS, "Match filters fetched", "Match filters fetched",
                    entries.stream().map(CategoryProfileRegistry::toVO).toList());
        } catch (MatchmakingException ex) {
            throw ex;
        } catch (Exception ex) {
            log.error("ALERT_FOR_ERROR: Error fetching match filters. Error: {}", ex.getMessage(), ex);
            throw new MatchmakingException("Error fetching match filters: " + ex.getMessage(), UNKNOWN_EXCEPTION);
        }
    }

    private void upsertRegistry(MatchFilterVO vo, MatchCategory category) {
        CategoryProfileRegistry registry = registryRepository
                .findByCognitoSubAndMatchCategory(vo.getCognitoSub(), category)
                .orElse(CategoryProfileRegistry.builder()
                        .cognitoSub(vo.getCognitoSub())
                        .matchCategory(category)
                        .createdAt(LocalDateTime.now())
                        .build());

        registry.setPreferredGender(vo.getPreferredGender());
        registry.setPreferredCity(vo.getPreferredCity());
        registry.setPreferredState(vo.getPreferredState());
        registry.setPreferredCountry(vo.getPreferredCountry());
        registry.setMaxTimezoneOffsetHours(vo.getMaxTimezoneOffsetHours());
        if (vo.getSameCompanyAllowed() != null) registry.setSameCompanyAllowed(vo.getSameCompanyAllowed());
        registry.setIsActive(true);
        registryRepository.save(registry);
        log.trace("CategoryProfileRegistry upserted for cognitoSub: {} category: {}", vo.getCognitoSub(), category);
    }

    private void routeToExtProcessor(MatchFilterVO vo, MatchCategory category) throws MatchmakingException {
        switch (category) {
            case PROFESSIONAL_MATRIMONY -> matrimonialProcessor.upsertFromFilter(vo);
            case CASUAL_DATING         -> datingProcessor.upsertFromFilter(vo);
            case FITNESS_SPORTS        -> fitnessProcessor.upsertFromFilter(vo);
            case FLATMATE_FINDER       -> flatmateProcessor.upsertFromFilter(vo);
            case GAMING_BUDDIES        -> gamingProcessor.upsertFromFilter(vo);
            case MENTORSHIP            -> professionalProcessor.upsertFromFilter(vo);
            case TRAVEL_TREKKING       -> travelProcessor.upsertFromFilter(vo);
            default                    -> log.debug("No ext profile processor for category: {}", category);
        }
    }
}
