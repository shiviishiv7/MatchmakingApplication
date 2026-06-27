package com.shiviishiv7.matchmaking.processor.post;

import com.shiviishiv7.matchmaking.common.enums.MatchCategory;
import com.shiviishiv7.matchmaking.processor.userprofile.*;
import com.shiviishiv7.matchmaking.provider.vo.MatchFilterVO;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;

@Service
@Slf4j
@RequiredArgsConstructor
public class PostProfileUpsertService implements IPostProfileUpsertService {

    private final IMatrimonialExtProfileProcessor matrimonialExtProfileProcessor;
    private final IDatingExtProfileProcessor datingExtProfileProcessor;
    private final IFitnessExtProfileProcessor fitnessExtProfileProcessor;
    private final IFlatmateExtProfileProcessor flatmateExtProfileProcessor;
    private final IGamingExtProfileProcessor gamingExtProfileProcessor;
    private final IProfessionalExtProfileProcessor professionalExtProfileProcessor;
    private final ITravelExtProfileProcessor travelExtProfileProcessor;

    @Override
    public void upsert(MatchFilterVO vo) {
        MatchCategory category = MatchCategory.fromName(vo.getChildCategory());
        if (category == null) return;

        switch (category.getParentGroup()) {
            case "Personal & Lifestyle Connections" -> {
                if (category == MatchCategory.PROFESSIONAL_MATRIMONY) {
                    matrimonialExtProfileProcessor.upsertFromFilter(vo);
                } else if (category == MatchCategory.CASUAL_DATING) {
                    datingExtProfileProcessor.upsertFromFilter(vo);
                }
            }
            case "Health & Wellness", "Shared Activities & Daily Routines" -> {
                if (category == MatchCategory.GYM_PARTNER || category == MatchCategory.YOGA_MEDITATION
                        || category == MatchCategory.CYCLING_RUNNING || category == MatchCategory.FITNESS_SPORTS) {
                    fitnessExtProfileProcessor.upsertFromFilter(vo);
                } else if (category == MatchCategory.GAMING_BUDDIES) {
                    gamingExtProfileProcessor.upsertFromFilter(vo);
                }
            }
            case "Travel & Exploration" -> {
                if (category == MatchCategory.FLATMATE_FINDER) {
                    flatmateExtProfileProcessor.upsertFromFilter(vo);
                } else {
                    travelExtProfileProcessor.upsertFromFilter(vo);
                }
            }
            case "Professional & Career Growth" -> professionalExtProfileProcessor.upsertFromFilter(vo);
            default -> log.warn("No ext profile upsert mapped for category: {}", category);
        }
    }
}
