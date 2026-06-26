package com.shiviishiv7.matchmaking.service.email;

import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.mail.javamail.JavaMailSender;
import org.springframework.mail.javamail.MimeMessageHelper;
import org.springframework.stereotype.Service;

@Service
@Slf4j
public class EmailService {

    private final JavaMailSender mailSender;

    @Value("${app.mail.from}")
    private String from;

    @Value("${app.mail.enabled:false}")
    private boolean enabled;

    public EmailService(JavaMailSender mailSender) {
        this.mailSender = mailSender;
    }

    /**
     * Sent to the user who submitted the post when no candidate is currently online.
     * Tells them their match has been saved and they'll be notified when the match comes online.
     */
    public void sendMatchSavedEmail(String toEmail, String toName, String matchedUserName) {
        send(toEmail, "You have a new match on Matchmaking!",
                matchSavedHtml(toName, matchedUserName));
    }

    /**
     * Sent to the matched candidate to let them know someone is waiting to connect.
     */
    public void sendMatchWaitingEmail(String toEmail, String toName, String requesterName) {
        send(toEmail, "Someone wants to connect with you!",
                matchWaitingHtml(toName, requesterName));
    }

    // ── private ───────────────────────────────────────────────────────────────

    private void send(String to, String subject, String html) {
        if (!enabled) {
            log.info("Email disabled — would have sent '{}' to {}", subject, to);
            return;
        }
        try {
            var message = mailSender.createMimeMessage();
            var helper = new MimeMessageHelper(message, false, "UTF-8");
            helper.setFrom(from);
            helper.setTo(to);
            helper.setSubject(subject);
            helper.setText(html, true);
            mailSender.send(message);
            log.info("Email sent: '{}' → {}", subject, to);
        } catch (Exception ex) {
            log.error("ALERT_FOR_ERROR: Failed to send email to {}: {}", to, ex.getMessage(), ex);
        }
    }

    private String matchSavedHtml(String name, String matchedName) {
        return """
                <p>Hi %s,</p>
                <p>Great news! We found a match for you: <strong>%s</strong>.</p>
                <p>They are not online right now, but we will notify you as soon as they come online so you can connect via a live video call.</p>
                <p>You can also open the app at any time and request your next match.</p>
                <br/><p>— The Matchmaking Team</p>
                """.formatted(name, matchedName);
    }

    private String matchWaitingHtml(String name, String requesterName) {
        return """
                <p>Hi %s,</p>
                <p><strong>%s</strong> has been matched with you and is looking to connect!</p>
                <p>Open the app and click <strong>"Next Match"</strong> to start a live video call.</p>
                <br/><p>— The Matchmaking Team</p>
                """.formatted(name, requesterName);
    }
}
