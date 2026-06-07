package com.shiviishiv7.matchmaking.common.security;

import com.shiviishiv7.matchmaking.common.exception.MatchmakingException;
import com.shiviishiv7.matchmaking.common.util.security.CurrentUserContext;
import com.shiviishiv7.matchmaking.common.util.security.CurrentUserDetails;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Component;

import static com.shiviishiv7.matchmaking.common.constants.MatchmakingHttpStatus.UNAUTHORIZED;

@Component
@Slf4j
public class MatchmakingSecurityUtility {

    /**
     * Returns the Cognito sub (JWT subject) set by JwtAuthenticationFilter for the current request.
     */
    public String getAuthenticatedUserSub() throws MatchmakingException {
        CurrentUserDetails currentUser = CurrentUserContext.getCurrentUser();
        if (currentUser == null || currentUser.getUsername() == null || currentUser.getUsername().isBlank()) {
            log.error("ALERT_FOR_ERROR: No authenticated user found in current request context.");
            throw new MatchmakingException("User is not authenticated", UNAUTHORIZED);
        }
        return currentUser.getUsername();
    }
}
