package com.nila.chores.notification;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;

import java.net.URI;
import java.net.http.HttpClient;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;
import java.time.Duration;

/**
 * Channel: sends reminder messages via the Telegram Bot API.
 * Adapted from the reminders app's TelegramSender — chores is a separate app/repo
 * so this is a direct copy-adapt, not a shared library.
 */
@Component
public class TelegramSender {

    private static final Logger log = LoggerFactory.getLogger(TelegramSender.class);
    private static final String TELEGRAM_API_BASE = "https://api.telegram.org/bot";

    private final String botToken;
    private final HttpClient httpClient;
    private final ObjectMapper objectMapper;

    public TelegramSender(@Value("${chores.telegram.bot-token:}") String botToken,
                          ObjectMapper objectMapper) {
        // Strip whitespace/newlines that k8s Secret injection can append.
        this.botToken = (botToken == null ? "" : botToken.strip());
        this.objectMapper = objectMapper;
        this.httpClient = HttpClient.newBuilder()
                .connectTimeout(Duration.ofSeconds(10))
                .build();
    }

    public boolean isConfigured() {
        return botToken != null && !botToken.isBlank();
    }

    /**
     * Sends a text message to a Telegram chat.
     *
     * @param chatId  Telegram chat ID (numeric string, e.g. "-5139466273")
     * @param subject subject line (prepended to body with a blank line)
     * @param body    message body
     * @return {@link SendResult} indicating success or failure with error detail
     */
    public SendResult send(String chatId, String subject, String body) {
        log.info("event=telegram.send.start chatId={}", chatId);

        if (!isConfigured()) {
            log.warn("event=telegram.send.skipped chatId={} reason=bot-token-not-configured", chatId);
            return SendResult.fail("Telegram bot token not configured");
        }

        String text = (subject == null || subject.isBlank()) ? body : subject + "\n\n" + body;

        // Build JSON manually; Jackson only used for response parsing.
        String escapedText = text.replace("\\", "\\\\").replace("\"", "\\\"")
                .replace("\n", "\\n").replace("\r", "\\r").replace("\t", "\\t");
        String escapedChatId = chatId.replace("\"", "\\\"");
        String jsonBody = "{\"chat_id\":\"" + escapedChatId + "\",\"text\":\"" + escapedText + "\"}";

        String url = TELEGRAM_API_BASE + botToken + "/sendMessage";

        try {
            HttpRequest request = HttpRequest.newBuilder()
                    .uri(URI.create(url))
                    .header("Content-Type", "application/json")
                    .timeout(Duration.ofSeconds(15))
                    .POST(HttpRequest.BodyPublishers.ofString(jsonBody))
                    .build();

            long start = System.currentTimeMillis();
            HttpResponse<String> response = httpClient.send(request, HttpResponse.BodyHandlers.ofString());
            long elapsed = System.currentTimeMillis() - start;

            int status = response.statusCode();
            String responseBody = response.body();

            boolean ok;
            String description = null;
            try {
                JsonNode root = objectMapper.readTree(responseBody);
                ok = root.path("ok").asBoolean(false);
                JsonNode descNode = root.path("description");
                if (!descNode.isMissingNode()) description = descNode.asText();
            } catch (Exception parseEx) {
                log.warn("event=telegram.send.parse-error chatId={} httpStatus={} durationMs={} reason={} rawBody={}",
                        chatId, status, elapsed, parseEx.getMessage(), responseBody);
                return SendResult.fail("Failed to parse Telegram response: " + parseEx.getMessage());
            }

            if (ok) {
                log.info("event=telegram.send.success chatId={} httpStatus={} durationMs={}",
                        chatId, status, elapsed);
                return SendResult.success();
            } else {
                log.warn("event=telegram.send.api-failure chatId={} httpStatus={} durationMs={} description={} response={}",
                        chatId, status, elapsed, description, responseBody);
                return SendResult.fail("Telegram API returned ok:false — " + (description != null ? description : responseBody));
            }
        } catch (InterruptedException e) {
            Thread.currentThread().interrupt();
            log.error("event=telegram.send.interrupted chatId={} reason={}", chatId, e.getMessage(), e);
            return SendResult.fail("Send interrupted: " + e.getMessage());
        } catch (Exception e) {
            log.error("event=telegram.send.error chatId={} reason={}", chatId, e.getMessage(), e);
            return SendResult.fail(e.getMessage());
        }
    }
}
