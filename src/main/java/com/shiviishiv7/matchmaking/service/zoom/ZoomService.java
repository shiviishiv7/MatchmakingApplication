package com.shiviishiv7.matchmaking.service.zoom;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;

import java.net.URI;
import java.net.http.HttpClient;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;
import java.time.LocalDateTime;
import java.time.ZoneId;
import java.time.format.DateTimeFormatter;
import java.util.Base64;
import java.util.Map;

@Service
@Slf4j
public class ZoomService {

    private static final String ZOOM_TOKEN_URL = "https://zoom.us/oauth/token";
    private static final String ZOOM_API_BASE  = "https://api.zoom.us/v2";
    private static final ZoneId IST = ZoneId.of("Asia/Kolkata");
    private static final DateTimeFormatter ZOOM_DT = DateTimeFormatter.ofPattern("yyyy-MM-dd'T'HH:mm:ss");

    @Value("${zoom.account.id}")
    private String accountId;

    @Value("${zoom.client.id}")
    private String clientId;

    @Value("${zoom.client.secret}")
    private String clientSecret;

    @Value("${zoom.host.email}")
    private String hostEmail;

    private final ObjectMapper objectMapper;
    private final HttpClient httpClient = HttpClient.newHttpClient();

    public ZoomService(ObjectMapper objectMapper) {
        this.objectMapper = objectMapper;
    }

    public ZoomMeetingResult createMeeting(String topic, LocalDateTime scheduledAt, int durationMinutes) {
        try {
            String token = fetchAccessToken();
            return createZoomMeeting(token, topic, scheduledAt, durationMinutes);
        } catch (Exception ex) {
            log.error("ALERT_FOR_ERROR: Failed to create Zoom meeting: {}", ex.getMessage(), ex);
            throw new RuntimeException("Zoom meeting creation failed: " + ex.getMessage(), ex);
        }
    }

    // ── private ───────────────────────────────────────────────────────────────

    private String fetchAccessToken() throws Exception {
        String credentials = Base64.getEncoder()
                .encodeToString((clientId + ":" + clientSecret).getBytes());

        String url = ZOOM_TOKEN_URL + "?grant_type=account_credentials&account_id=" + accountId;

        HttpRequest request = HttpRequest.newBuilder()
                .uri(URI.create(url))
                .header("Authorization", "Basic " + credentials)
                .header("Content-Type", "application/x-www-form-urlencoded")
                .POST(HttpRequest.BodyPublishers.noBody())
                .build();

        HttpResponse<String> response = httpClient.send(request, HttpResponse.BodyHandlers.ofString());

        if (response.statusCode() != 200) {
            throw new RuntimeException("Zoom token error: " + response.statusCode() + " " + response.body());
        }

        JsonNode root = objectMapper.readTree(response.body());
        return root.path("access_token").asText();
    }

    private ZoomMeetingResult createZoomMeeting(String token, String topic,
                                                 LocalDateTime scheduledAt, int durationMinutes) throws Exception {
        String startTimeIst = scheduledAt.atZone(IST).format(ZOOM_DT);

        Map<String, Object> body = Map.of(
                "topic", topic,
                "type", 2,
                "start_time", startTimeIst,
                "duration", durationMinutes,
                "timezone", "Asia/Kolkata",
                "settings", Map.of(
                        "host_video", true,
                        "participant_video", true,
                        "join_before_host", true,
                        "waiting_room", false,
                        "auto_recording", "none"
                )
        );

        String url = ZOOM_API_BASE + "/users/" + hostEmail + "/meetings";

        HttpRequest request = HttpRequest.newBuilder()
                .uri(URI.create(url))
                .header("Authorization", "Bearer " + token)
                .header("Content-Type", "application/json")
                .POST(HttpRequest.BodyPublishers.ofString(objectMapper.writeValueAsString(body)))
                .build();

        HttpResponse<String> response = httpClient.send(request, HttpResponse.BodyHandlers.ofString());

        if (response.statusCode() != 201) {
            throw new RuntimeException("Zoom create meeting error: " + response.statusCode() + " " + response.body());
        }

        JsonNode root = objectMapper.readTree(response.body());
        return ZoomMeetingResult.builder()
                .meetingId(root.path("id").asText())
                .joinUrl(root.path("join_url").asText())
                .startUrl(root.path("start_url").asText())
                .password(root.path("password").asText())
                .build();
    }

    @lombok.Builder
    @lombok.Getter
    public static class ZoomMeetingResult {
        private final String meetingId;
        private final String joinUrl;
        private final String startUrl;
        private final String password;
    }
}
