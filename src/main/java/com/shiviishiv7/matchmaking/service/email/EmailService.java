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

    public void sendMeetingScheduledEmail(String toEmail, String toName,
                                          String matchedName, String zoomJoinUrl,
                                          String scheduledAtFormatted) {
        send(toEmail, "Your Zoom meeting is confirmed on Shall We Connect!",
                meetingScheduledHtml(toName, matchedName, zoomJoinUrl, scheduledAtFormatted));
    }

    public void sendMatchSavedEmail(String toEmail, String toName, String matchedUserName) {
        send(toEmail, "You have a new match on Shall We Connect!",
                matchSavedHtml(toName, matchedUserName));
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
                <p>We'll notify you by email once a Zoom meeting is scheduled.</p>
                <br/><p>— The Shall We Connect Team</p>
                """.formatted(name, matchedName);
    }

    private String meetingScheduledHtml(String name, String matchedName,
                                         String zoomJoinUrl, String scheduledAt) {
        return """
                <div style="font-family:Arial,sans-serif;max-width:600px;margin:0 auto">
                  <h2 style="color:#6366f1">Your meeting is confirmed!</h2>
                  <p>Hi <strong>%s</strong>,</p>
                  <p>We've matched you with <strong>%s</strong> on Shall We Connect.</p>
                  <p>Your Zoom meeting is scheduled for:</p>
                  <p style="font-size:1.2em;font-weight:bold;color:#1e1b4b">%s (IST)</p>
                  <div style="margin:24px 0">
                    <a href="%s"
                       style="background:#6366f1;color:white;padding:12px 24px;
                              text-decoration:none;border-radius:6px;font-weight:bold">
                      Join Zoom Meeting
                    </a>
                  </div>
                  <p style="color:#666;font-size:0.9em">
                    Or copy this link: <a href="%s">%s</a>
                  </p>
                  <hr style="border:none;border-top:1px solid #eee;margin:24px 0"/>
                  <p style="color:#999;font-size:0.8em">
                    After the meeting, submit your feedback in the app.
                    If both of you want to connect again, we'll schedule another meeting.
                  </p>
                  <p>— The Shall We Connect Team</p>
                </div>
                """.formatted(name, matchedName, scheduledAt, zoomJoinUrl, zoomJoinUrl, zoomJoinUrl);
    }
}
