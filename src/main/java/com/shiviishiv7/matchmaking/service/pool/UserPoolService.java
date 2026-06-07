package com.shiviishiv7.matchmaking.service.pool;

import com.shiviishiv7.matchmaking.provider.vo.ws.PoolUserVO;
import org.springframework.stereotype.Service;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

/**
 * Maintains the pool of users currently available for WebRTC connection.
 * Uses ConcurrentHashMap so it is thread-safe across multiple WebSocket sessions.
 *
 * Key   = cognitoSub
 * Value = PoolUserVO (display info)
 */
@Service
public class UserPoolService {

    // cognitoSub → PoolUserVO
    private final Map<String, PoolUserVO> pool = new ConcurrentHashMap<>();

    /** Add user to the pool when they join. */
    public void addUser(PoolUserVO user) {
        pool.put(user.getCognitoSub(), user);
    }

    /** Remove user from pool when they disconnect or leave. */
    public void removeUser(String cognitoSub) {
        pool.remove(cognitoSub);
    }

    /** Returns all users in the pool EXCEPT the requesting user. */
    public List<PoolUserVO> getOtherUsers(String excludeSub) {
        List<PoolUserVO> others = new ArrayList<>();
        pool.forEach((sub, user) -> {
            if (!sub.equals(excludeSub)) {
                others.add(user);
            }
        });
        return others;
    }

    public boolean isInPool(String cognitoSub) {
        return pool.containsKey(cognitoSub);
    }
}
