package me.geetprince.Eventify.service;

import org.springframework.stereotype.Service;

import java.time.Instant;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

@Service
public class LoginRateLimiterService {

    private static final int MAX_ATTEMPTS = 5;
    private static final long LOCK_TIME_DURATION_MS = 15 * 60 * 1000; // 15 minutes lock

    private static class Attempt {
        int count;
        Instant lockedUntil;

        Attempt() {
            this.count = 1;
            this.lockedUntil = null;
        }
    }

    private final Map<String, Attempt> attemptsCache = new ConcurrentHashMap<>();

    public boolean isBlocked(String key) {
        if (key == null) return false;
        Attempt attempt = attemptsCache.get(key);
        if (attempt == null) {
            return false;
        }
        if (attempt.lockedUntil != null) {
            if (Instant.now().isBefore(attempt.lockedUntil)) {
                return true;
            } else {
                attemptsCache.remove(key);
                return false;
            }
        }
        return false;
    }

    public void loginFailed(String key) {
        if (key == null) return;
        attemptsCache.compute(key, (k, attempt) -> {
            if (attempt == null) {
                return new Attempt();
            }
            attempt.count++;
            if (attempt.count >= MAX_ATTEMPTS) {
                attempt.lockedUntil = Instant.now().plusMillis(LOCK_TIME_DURATION_MS);
            }
            return attempt;
        });
    }

    public void loginSucceeded(String key) {
        if (key == null) return;
        attemptsCache.remove(key);
    }
}
