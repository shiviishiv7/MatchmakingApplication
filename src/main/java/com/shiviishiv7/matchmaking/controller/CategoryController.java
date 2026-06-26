package com.shiviishiv7.matchmaking.controller;

import com.shiviishiv7.matchmaking.provider.vo.category.MatchCategoryGroupVO;
import com.shiviishiv7.matchmaking.provider.vo.category.MatchCategoryVO;
import com.shiviishiv7.matchmaking.service.category.CategoryCacheService;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequestMapping("/api/v1/categories")
@RequiredArgsConstructor
public class CategoryController {

    private final CategoryCacheService categoryCacheService;

    /** All active groups with nested categories — used by chat UI to show selection. */
    @GetMapping("/groups")
    public ResponseEntity<List<MatchCategoryGroupVO>> getAllGroups() {
        return ResponseEntity.ok(categoryCacheService.getAllGroupsWithCategories());
    }

    /** Flat list of all active categories. */
    @GetMapping
    public ResponseEntity<List<MatchCategoryVO>> getAllCategories() {
        return ResponseEntity.ok(categoryCacheService.getAllCategories());
    }

    /** Lookup by enumKey (e.g. GYM_PARTNER). */
    @GetMapping("/{enumKey}")
    public ResponseEntity<MatchCategoryVO> getByEnumKey(@PathVariable String enumKey) {
        return categoryCacheService.findByEnumKey(enumKey)
                .map(ResponseEntity::ok)
                .orElse(ResponseEntity.notFound().build());
    }

    /** Admin: evict all category caches (call after inserting new category via SQL). */
    @PostMapping("/cache/evict")
    public ResponseEntity<String> evictCache() {
        categoryCacheService.evictAll();
        return ResponseEntity.ok("Category caches cleared");
    }
}
