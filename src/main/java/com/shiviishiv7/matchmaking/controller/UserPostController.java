package com.shiviishiv7.matchmaking.controller;

import com.shiviishiv7.matchmaking.common.util.security.CurrentUserContext;
import com.shiviishiv7.matchmaking.processor.post.IPostAnalysisProcessor;
import com.shiviishiv7.matchmaking.provider.vo.BaseVO;
import com.shiviishiv7.matchmaking.provider.vo.post.PostAnalyzeRequestVO;
import com.shiviishiv7.matchmaking.provider.vo.post.PostSubmitRequestVO;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/api/v1/posts")
@RequiredArgsConstructor
@Slf4j
public class UserPostController {

    private final IPostAnalysisProcessor postAnalysisProcessor;

    /**
     * Step 1: User submits their draft post.
     * Returns inferred match category + dynamic follow-up questions.
     */
    @PostMapping("/analyze")
    public ResponseEntity<BaseVO> analyze(@Valid @RequestBody PostAnalyzeRequestVO request) {
        var result = postAnalysisProcessor.analyze(request.getPostText());
        return ResponseEntity.ok(new BaseVO(200, "Analysis complete.", null, result));
    }

    /**
     * Step 2: User submits the post along with answers to follow-up questions.
     * Saves the post and returns the final match category.
     */
    @PostMapping("/submit")
    public ResponseEntity<BaseVO> submit(@Valid @RequestBody PostSubmitRequestVO request) {
        String cognitoSub = CurrentUserContext.getCurrentUser().getUsername();
        var result = postAnalysisProcessor.submit(cognitoSub, request);
        return ResponseEntity.ok(new BaseVO(200, "Post saved successfully.", null, result));
    }
}
