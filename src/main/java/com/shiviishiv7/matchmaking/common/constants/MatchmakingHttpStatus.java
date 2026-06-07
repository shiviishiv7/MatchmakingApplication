package com.shiviishiv7.matchmaking.common.constants;

public class MatchmakingHttpStatus {

    private MatchmakingHttpStatus() {}

    public static final int SUCCESS                  = 200;
    public static final int VALIDATION_ERROR         = 400;
    public static final int UNAUTHORIZED             = 401;
    public static final int DATA_NOT_FOUND           = 404;
    public static final int DUPLICATE_RECORD         = 409;
    public static final int UNKNOWN_EXCEPTION        = 500;
}
