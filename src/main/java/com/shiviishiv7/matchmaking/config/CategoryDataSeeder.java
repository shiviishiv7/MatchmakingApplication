package com.shiviishiv7.matchmaking.config;

import com.shiviishiv7.matchmaking.common.enums.MatchCategory;
import com.shiviishiv7.matchmaking.provider.implementation.MatchCategoryEntityRepository;
import com.shiviishiv7.matchmaking.provider.implementation.MatchCategoryGroupRepository;
import com.shiviishiv7.matchmaking.provider.model.MatchCategoryEntity;
import com.shiviishiv7.matchmaking.provider.model.MatchCategoryGroup;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.boot.ApplicationArguments;
import org.springframework.boot.ApplicationRunner;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Transactional;

import java.util.HashMap;
import java.util.Map;
import java.util.concurrent.atomic.AtomicInteger;

/**
 * On startup, seeds MATCH_CATEGORY_GROUP and MATCH_CATEGORY tables from the MatchCategory enum
 * if they are empty. Safe to run repeatedly — skips existing rows by enumKey.
 *
 * Once the DB is populated, new categories can be added via SQL INSERT without code changes.
 */
@Slf4j
@Component
@RequiredArgsConstructor
public class CategoryDataSeeder implements ApplicationRunner {

    private final MatchCategoryGroupRepository groupRepository;
    private final MatchCategoryEntityRepository categoryRepository;

    @Override
    @Transactional
    public void run(ApplicationArguments args) {
        long existingCount = categoryRepository.count();
        if (existingCount > 0) {
            log.info("Category tables already seeded ({} categories). Skipping.", existingCount);
            return;
        }

        log.info("Seeding category tables from MatchCategory enum...");

        Map<String, MatchCategoryGroup> groupCache = new HashMap<>();
        AtomicInteger groupOrder = new AtomicInteger(1);
        AtomicInteger catOrder = new AtomicInteger(1);

        for (MatchCategory mc : MatchCategory.values()) {
            String groupName = mc.getParentGroup();

            MatchCategoryGroup group = groupCache.computeIfAbsent(groupName, name -> {
                MatchCategoryGroup g = groupRepository.findByNameIgnoreCase(name)
                        .orElseGet(() -> groupRepository.save(
                                MatchCategoryGroup.builder()
                                        .name(name)
                                        .displayOrder(groupOrder.getAndIncrement())
                                        .isActive(true)
                                        .build()
                        ));
                return g;
            });

            boolean alreadyExists = categoryRepository.findByEnumKeyIgnoreCase(mc.name()).isPresent();
            if (!alreadyExists) {
                categoryRepository.save(
                        MatchCategoryEntity.builder()
                                .group(group)
                                .enumKey(mc.name())
                                .displayName(mc.getDisplayName())
                                .displayOrder(catOrder.getAndIncrement())
                                .isActive(true)
                                .build()
                );
            }
        }

        log.info("Category seeding complete. {} groups, {} categories saved.",
                groupRepository.count(), categoryRepository.count());
    }
}
