package com.shiviishiv7.matchmaking.controller;

import com.shiviishiv7.matchmaking.common.util.security.CurrentUserContext;
import com.shiviishiv7.matchmaking.common.enums.PostStatus;
import com.shiviishiv7.matchmaking.processor.post.PostAnalysisProcessor;
import com.shiviishiv7.matchmaking.processor.post.IPostAnalysisProcessor;
import com.shiviishiv7.matchmaking.provider.implementation.UserPostRepository;
import com.shiviishiv7.matchmaking.provider.model.UserPost;
import com.shiviishiv7.matchmaking.provider.vo.BaseVO;
import com.shiviishiv7.matchmaking.provider.vo.post.PostAnalyzeRequestVO;
import com.shiviishiv7.matchmaking.provider.vo.post.PostSubmitRequestVO;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequestMapping("/api/v1/posts")
@RequiredArgsConstructor
@Slf4j
public class UserPostController {

    private final IPostAnalysisProcessor postAnalysisProcessor;
    private final PostAnalysisProcessor postAnalysisProcessorImpl;
    private final UserPostRepository userPostRepository;

    /**
     * Step 1: Analyze the post — returns only the fixed questions not yet answered in the post.
     */
    @PostMapping("/analyze")
    public ResponseEntity<BaseVO> analyze(@Valid @RequestBody PostAnalyzeRequestVO request) {
        var result = postAnalysisProcessor.analyze(request.getPostText(), request.getIntent());
        return ResponseEntity.ok(new BaseVO(200, "Analysis complete.", null, result));
    }

    /**
     * Step 2: Submit post + answers + partner preferences. Queued for matching.
     */
    @PostMapping("/submit")
    public ResponseEntity<BaseVO> submit(@Valid @RequestBody PostSubmitRequestVO request) {
        String cognitoSub = CurrentUserContext.getCurrentUser().getUsername();
        var result = postAnalysisProcessor.submit(cognitoSub, request);
        return ResponseEntity.ok(new BaseVO(200,
                "Post saved! We'll notify you by email when a match is found.", null, result));
    }

    /**
     * Get all posts for the current user.
     */
    @GetMapping("/my")
    public ResponseEntity<BaseVO> myPosts() {
        String cognitoSub = CurrentUserContext.getCurrentUser().getUsername();
        List<UserPost> posts = userPostRepository.findByCognitoSubOrderByCreatedAtDesc(cognitoSub);
        return ResponseEntity.ok(new BaseVO(200, "Posts fetched.", null, posts));
    }

    /**
     * Get only ACTIVE posts for the current user.
     */
    @GetMapping("/my/active")
    public ResponseEntity<BaseVO> myActivePosts() {
        String cognitoSub = CurrentUserContext.getCurrentUser().getUsername();
        List<UserPost> posts = userPostRepository.findByCognitoSubAndStatusOrderByCreatedAtDesc(
                cognitoSub, PostStatus.ACTIVE);
        return ResponseEntity.ok(new BaseVO(200, "Active posts fetched.", null, posts));
    }

    /**
     * Manually close/delete a post.
     */
    @DeleteMapping("/{postId}")
    public ResponseEntity<BaseVO> closePost(@PathVariable Long postId) {
        String cognitoSub = CurrentUserContext.getCurrentUser().getUsername();
        postAnalysisProcessorImpl.closePost(postId, cognitoSub);
        return ResponseEntity.ok(new BaseVO(200, "Post closed successfully.", null, null));
    }
}
