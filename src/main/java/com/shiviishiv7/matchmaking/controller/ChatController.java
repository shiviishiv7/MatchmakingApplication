package com.shiviishiv7.matchmaking.controller;

import com.shiviishiv7.matchmaking.common.security.MatchmakingSecurityUtility;
import com.shiviishiv7.matchmaking.provider.vo.chat.ChatMessageRequest;
import com.shiviishiv7.matchmaking.provider.vo.chat.ChatSessionVO;
import com.shiviishiv7.matchmaking.service.chat.ChatService;
import lombok.RequiredArgsConstructor;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.servlet.mvc.method.annotation.SseEmitter;

import java.util.List;
import java.util.Map;

@RestController
@RequestMapping("/api/v1/chat")
@RequiredArgsConstructor
public class ChatController {

    private final ChatService chatService;
    private final MatchmakingSecurityUtility securityUtility;

    /** Start a new chat session. Returns session details + opening bot message. */
    @PostMapping("/session/start")
    public ResponseEntity<ChatSessionVO> startSession() {
        String cognitoSub = securityUtility.getAuthenticatedUserSub();
        return ResponseEntity.ok(chatService.startSession(cognitoSub));
    }

    /** Send a user message; streams bot response token by token via SSE. */
    @PostMapping(value = "/session/{sessionId}/message", produces = MediaType.TEXT_EVENT_STREAM_VALUE)
    public SseEmitter sendMessage(@PathVariable Long sessionId,
                                  @RequestBody ChatMessageRequest request) {
        String cognitoSub = securityUtility.getAuthenticatedUserSub();
        return chatService.sendMessage(cognitoSub, sessionId, request.getContent());
    }

    /** Get a session with full conversation history (for resume). */
    @GetMapping("/session/{sessionId}")
    public ResponseEntity<ChatSessionVO> getSession(@PathVariable Long sessionId) {
        String cognitoSub = securityUtility.getAuthenticatedUserSub();
        return ResponseEntity.ok(chatService.getSession(cognitoSub, sessionId));
    }

    /** List all sessions for the logged-in user (history screen). */
    @GetMapping("/sessions")
    public ResponseEntity<List<ChatSessionVO>> listSessions() {
        String cognitoSub = securityUtility.getAuthenticatedUserSub();
        return ResponseEntity.ok(chatService.listSessions(cognitoSub));
    }

    /** Mark session as SUBMITTED and return collectedAttributes for posting to match engine. */
    @PostMapping("/session/{sessionId}/submit")
    public ResponseEntity<Map<String, Object>> submitSession(@PathVariable Long sessionId) {
        String cognitoSub = securityUtility.getAuthenticatedUserSub();
        return ResponseEntity.ok(chatService.submitSession(cognitoSub, sessionId));
    }
}
