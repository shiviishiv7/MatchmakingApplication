package com.shiviishiv7.matchmaking.service.category;

import com.shiviishiv7.matchmaking.provider.implementation.MatchCategoryEntityRepository;
import com.shiviishiv7.matchmaking.provider.implementation.MatchCategoryGroupRepository;
import com.shiviishiv7.matchmaking.provider.model.MatchCategoryEntity;
import com.shiviishiv7.matchmaking.provider.model.MatchCategoryGroup;
import com.shiviishiv7.matchmaking.provider.vo.category.MatchCategoryGroupVO;
import com.shiviishiv7.matchmaking.provider.vo.category.MatchCategoryVO;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.cache.annotation.CacheEvict;
import org.springframework.cache.annotation.Cacheable;
import org.springframework.stereotype.Service;

import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.stream.Collectors;

@Slf4j
@Service
@RequiredArgsConstructor
public class CategoryCacheService {

    private final MatchCategoryGroupRepository groupRepository;
    private final MatchCategoryEntityRepository categoryRepository;

    /** Returns all active groups with their categories. Cached; rarely changes. */
    @Cacheable(value = "categoryGroups")
    public List<MatchCategoryGroupVO> getAllGroupsWithCategories() {
        log.debug("Loading category groups from DB");
        List<MatchCategoryGroup> groups = groupRepository.findAllByIsActiveTrueOrderByDisplayOrderAsc();
        List<MatchCategoryEntity> allCategories = categoryRepository.findAllByIsActiveTrueOrderByDisplayOrderAsc();

        Map<Integer, List<MatchCategoryVO>> byGroup = allCategories.stream()
                .collect(Collectors.groupingBy(
                        c -> c.getGroup().getId(),
                        Collectors.mapping(MatchCategoryVO::from, Collectors.toList())
                ));

        return groups.stream()
                .map(g -> MatchCategoryGroupVO.from(g, byGroup.getOrDefault(g.getId(), List.of())))
                .collect(Collectors.toList());
    }

    /** Returns all active categories as a flat list. Cached. */
    @Cacheable(value = "allCategories")
    public List<MatchCategoryVO> getAllCategories() {
        log.debug("Loading all categories from DB");
        return categoryRepository.findAllByIsActiveTrueOrderByDisplayOrderAsc()
                .stream()
                .map(MatchCategoryVO::from)
                .collect(Collectors.toList());
    }

    /** Lookup a single category by its enumKey (e.g. "GYM_PARTNER"). Cached. */
    @Cacheable(value = "categoryByKey", key = "#enumKey.toUpperCase()")
    public Optional<MatchCategoryVO> findByEnumKey(String enumKey) {
        return categoryRepository.findByEnumKeyIgnoreCase(enumKey)
                .map(MatchCategoryVO::from);
    }

    /** Call this after any admin add/update to refresh caches. */
    @CacheEvict(value = {"categoryGroups", "allCategories", "categoryByKey"}, allEntries = true)
    public void evictAll() {
        log.info("Category caches evicted");
    }
}
