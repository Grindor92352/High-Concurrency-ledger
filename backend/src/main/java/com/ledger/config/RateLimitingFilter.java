package com.ledger.config;

import jakarta.servlet.FilterChain;
import jakarta.servlet.ServletException;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import org.springframework.web.filter.OncePerRequestFilter;

import java.io.IOException;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.atomic.AtomicInteger;

/**
 * A deliberately simple, dependency-free fixed-window rate limiter, applied
 * only to /api/auth/** (see RateLimitingConfig's urlPatterns). Its job is to
 * blunt credential-stuffing / brute-force login attempts and registration
 * spam — it is NOT a substitute for a distributed limiter (e.g. Redis-backed
 * token bucket) in a real multi-instance deployment, where each instance
 * would otherwise track its own independent counters. Documented as a known
 * limitation in the README.
 *
 * Not registered as a Spring @Component — it's wired up manually via a
 * FilterRegistrationBean in RateLimitingConfig so it only intercepts the
 * auth paths, not every request in the app.
 */
public class RateLimitingFilter extends OncePerRequestFilter {

    private static final int MAX_REQUESTS_PER_WINDOW = 10;
    private static final long WINDOW_MILLIS = 60_000; // 1 minute

    private final ConcurrentHashMap<String, RequestWindow> windowsByClient = new ConcurrentHashMap<>();

    @Override
    protected void doFilterInternal(HttpServletRequest request,
                                     HttpServletResponse response,
                                     FilterChain filterChain) throws ServletException, IOException {

        String clientKey = resolveClientKey(request);
        RequestWindow window = windowsByClient.computeIfAbsent(clientKey, k -> new RequestWindow());

        if (!window.tryConsume()) {
            response.setStatus(429); // 429 Too Many Requests
            response.setHeader("Retry-After", "60");
            response.setContentType("application/json");
            response.getWriter().write(
                    "{\"status\":429,\"error\":\"Too Many Requests\"," +
                            "\"message\":\"Rate limit exceeded for authentication endpoints. Try again in a minute.\"}");
            return;
        }

        filterChain.doFilter(request, response);
    }

    /**
     * Prefers X-Forwarded-For (set by a reverse proxy/load balancer in front
     * of the app) over the raw socket address, since in a real deployment
     * request.getRemoteAddr() would just be the proxy's own IP for every
     * request.
     */
    private String resolveClientKey(HttpServletRequest request) {
        String forwardedFor = request.getHeader("X-Forwarded-For");
        if (forwardedFor != null && !forwardedFor.isBlank()) {
            return forwardedFor.split(",")[0].trim();
        }
        return request.getRemoteAddr();
    }

    /** Fixed-window counter: resets to zero whenever the window has elapsed. */
    private static class RequestWindow {
        private final AtomicInteger count = new AtomicInteger(0);
        private volatile long windowStart = System.currentTimeMillis();

        synchronized boolean tryConsume() {
            long now = System.currentTimeMillis();
            if (now - windowStart > WINDOW_MILLIS) {
                windowStart = now;
                count.set(0);
            }
            return count.incrementAndGet() <= MAX_REQUESTS_PER_WINDOW;
        }
    }
}
