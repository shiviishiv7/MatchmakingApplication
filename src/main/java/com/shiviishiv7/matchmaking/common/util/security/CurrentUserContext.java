package com.shiviishiv7.matchmaking.common.util.security;


public class CurrentUserContext {
    private static final ThreadLocal<CurrentUserDetails> currentUser = new ThreadLocal<>();

    public static void setCurrentUser(CurrentUserDetails currentUser) {
        CurrentUserContext.currentUser.set(currentUser);
    }

    public static CurrentUserDetails getCurrentUser() {
        return currentUser.get();
    }

    public static void clear() {
        currentUser.remove();
    }
}


