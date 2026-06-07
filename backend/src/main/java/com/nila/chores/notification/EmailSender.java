package com.nila.chores.notification;

import jakarta.mail.internet.InternetAddress;
import jakarta.mail.internet.MimeMessage;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.mail.javamail.JavaMailSender;
import org.springframework.mail.javamail.MimeMessageHelper;
import org.springframework.stereotype.Component;

import java.nio.charset.StandardCharsets;

/**
 * Channel: sends plain-text reminder emails via Spring JavaMailSender (SMTP).
 * Adapted from the reminders app's EmailSender — chores is a separate app/repo
 * so this is a direct copy-adapt, not a shared library.
 */
@Component
public class EmailSender {

    private static final Logger log = LoggerFactory.getLogger(EmailSender.class);

    private final JavaMailSender mailSender;
    private final String username;
    private final String from;
    private final String fromName;

    public EmailSender(JavaMailSender mailSender,
                       @Value("${spring.mail.username:}") String username,
                       @Value("${chores.email.from:}") String from,
                       @Value("${chores.email.from-name:Chores}") String fromName) {
        this.mailSender = mailSender;
        this.username = username;
        this.from = from.isBlank() ? username : from;
        this.fromName = fromName;
    }

    public boolean isConfigured() {
        return username != null && !username.isBlank();
    }

    /**
     * Sends a plain-text email.
     *
     * @param to      recipient email address
     * @param subject email subject line
     * @param body    plain-text body
     * @return {@link SendResult} indicating success or failure with error detail
     */
    public SendResult send(String to, String subject, String body) {
        if (!isConfigured()) {
            log.warn("event=email.send.skipped to=<redacted> reason=not-configured");
            return SendResult.fail("Email not configured");
        }
        log.info("event=email.send.start to=<redacted>@{} subject={}", domainOf(to), subject);
        try {
            MimeMessage mime = mailSender.createMimeMessage();
            MimeMessageHelper helper = new MimeMessageHelper(mime, false, StandardCharsets.UTF_8.name());
            helper.setFrom(new InternetAddress(from, fromName, StandardCharsets.UTF_8.name()));
            helper.setTo(to);
            helper.setSubject(subject);
            helper.setText(body, false);
            mailSender.send(mime);
            log.info("event=email.send.success to=<redacted>@{}", domainOf(to));
            return SendResult.success();
        } catch (Exception e) {
            log.warn("event=email.send.failed to=<redacted>@{} reason={}", domainOf(to), e.getMessage(), e);
            return SendResult.fail(e.getMessage());
        }
    }

    private static String domainOf(String address) {
        if (address == null) return "unknown";
        int at = address.indexOf('@');
        return at >= 0 ? address.substring(at + 1) : "unknown";
    }
}
