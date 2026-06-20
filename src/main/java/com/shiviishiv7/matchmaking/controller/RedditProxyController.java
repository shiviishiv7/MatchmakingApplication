package com.shiviishiv7.matchmaking.controller;

import io.swagger.v3.oas.annotations.tags.Tag;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.*;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.client.RestTemplate;

/**
 * Proxies Reddit JSON API calls to avoid browser CORS / User-Agent restrictions.
 * Reddit blocks direct browser requests that lack a proper User-Agent header
 * (browsers forbid setting User-Agent via JS).  All Reddit traffic routes
 * through this endpoint instead.
 */
@RestController
@RequestMapping("/reddit")
@Slf4j
@Tag(name = "Reddit Proxy", description = "Proxy for Reddit JSON API")
public class RedditProxyController {

    private static final String REDDIT_BASE  = "https://www.reddit.com/r/";
    private static final String USER_AGENT   = "ShallWeConnect/1.0 (matchmaking app; contact: admin@shallweconnect.online)";

    private final RestTemplate restTemplate = new RestTemplate();

    /**
     * GET /reddit/memes?subreddit=memes&sort=hot&t=day&limit=50
     */
    @GetMapping("/memes")
    public ResponseEntity<String> getMemes(
            @RequestParam(defaultValue = "memes")  String subreddit,
            @RequestParam(defaultValue = "hot")     String sort,
            @RequestParam(defaultValue = "day")     String t,
            @RequestParam(defaultValue = "50")      int    limit) {

        String url = buildUrl(subreddit, sort, t, limit);
        log.info("[Reddit] Proxying: {}", url);

        HttpHeaders headers = new HttpHeaders();
        headers.set(HttpHeaders.USER_AGENT, USER_AGENT);
        headers.set(HttpHeaders.ACCEPT,     MediaType.APPLICATION_JSON_VALUE);

        try {
            ResponseEntity<String> response = restTemplate.exchange(
                    url, HttpMethod.GET, new HttpEntity<>(headers), String.class);
            return ResponseEntity.ok()
                    .contentType(MediaType.APPLICATION_JSON)
                    .body(response.getBody());
        } catch (Exception ex) {
            log.error("[Reddit] Proxy request failed: {}", ex.getMessage());
            return ResponseEntity.status(HttpStatus.BAD_GATEWAY)
                    .body("{\"error\":\"Failed to fetch from Reddit\"}");
        }
    }

    /**
     * GET /reddit/random?subreddit=memes
     */
    @GetMapping("/random")
    public ResponseEntity<String> getRandom(
            @RequestParam(defaultValue = "memes") String subreddit) {

        String url = REDDIT_BASE + subreddit + "/random.json";
        log.info("[Reddit] Proxying random: {}", url);

        HttpHeaders headers = new HttpHeaders();
        headers.set(HttpHeaders.USER_AGENT, USER_AGENT);
        headers.set(HttpHeaders.ACCEPT,     MediaType.APPLICATION_JSON_VALUE);

        try {
            ResponseEntity<String> response = restTemplate.exchange(
                    url, HttpMethod.GET, new HttpEntity<>(headers), String.class);
            return ResponseEntity.ok()
                    .contentType(MediaType.APPLICATION_JSON)
                    .body(response.getBody());
        } catch (Exception ex) {
            log.error("[Reddit] Random proxy failed: {}", ex.getMessage());
            return ResponseEntity.status(HttpStatus.BAD_GATEWAY)
                    .body("{\"error\":\"Failed to fetch random meme\"}");
        }
    }

    private String buildUrl(String subreddit, String sort, String t, int limit) {
        String base = REDDIT_BASE + subreddit + "/" + sort + ".json?limit=" + limit;
        return "top".equals(sort) ? base + "&t=" + t : base;
    }
}
