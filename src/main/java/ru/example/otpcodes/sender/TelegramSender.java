package ru.example.otpcodes.sender;

import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Component;
import ru.example.otpcodes.domain.Channel;

import java.io.IOException;
import java.io.InputStream;
import java.net.URI;
import java.net.URLEncoder;
import java.net.http.HttpClient;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;
import java.nio.charset.StandardCharsets;
import java.time.Duration;
import java.util.Properties;

@Slf4j
@Component
public class TelegramSender implements NotificationSender {

    private final String apiUrlTemplate;
    private final String botToken;
    private final String defaultChatId;
    private final HttpClient httpClient = HttpClient.newBuilder()
            .connectTimeout(Duration.ofSeconds(5))
            .build();

    public TelegramSender() {
        Properties config = loadConfig();
        this.botToken = config.getProperty("telegram.bot_token");
        this.defaultChatId = config.getProperty("telegram.chat_id");
        this.apiUrlTemplate = config.getProperty("telegram.api_url");
    }

    @Override
    public Channel channel() {
        return Channel.TELEGRAM;
    }

    @Override
    public void send(String destination, String code) {
        String chatId = (destination != null && !destination.isBlank()) ? destination : defaultChatId;
        String message = "Your confirmation code is: " + code;
        String url = String.format("%s?chat_id=%s&text=%s",
                String.format(apiUrlTemplate, botToken),
                urlEncode(chatId),
                urlEncode(message));

        HttpRequest request = HttpRequest.newBuilder()
                .uri(URI.create(url))
                .timeout(Duration.ofSeconds(10))
                .GET()
                .build();

        try {
            HttpResponse<String> response = httpClient.send(request, HttpResponse.BodyHandlers.ofString());
            if (response.statusCode() != 200) {
                log.error("Telegram API returned status {}: {}", response.statusCode(), response.body());
                throw new RuntimeException("Telegram API error, status=" + response.statusCode());
            }
            log.info("Telegram message sent to chatId={}", chatId);
        } catch (InterruptedException e) {
            Thread.currentThread().interrupt();
            throw new RuntimeException("Telegram send interrupted", e);
        } catch (IOException e) {
            log.error("Telegram send failed: {}", e.getMessage(), e);
            throw new RuntimeException("Failed to send Telegram message", e);
        }
    }

    private static String urlEncode(String value) {
        return URLEncoder.encode(value, StandardCharsets.UTF_8);
    }

    private Properties loadConfig() {
        Properties props = new Properties();
        try (InputStream is = TelegramSender.class.getClassLoader().getResourceAsStream("telegram.properties")) {
            if (is == null) {
                throw new IllegalStateException("telegram.properties not found on classpath");
            }
            props.load(is);
        } catch (IOException e) {
            throw new RuntimeException("Failed to load telegram configuration", e);
        }
        overrideFromEnv(props, "telegram.bot_token", "TELEGRAM_BOT_TOKEN");
        overrideFromEnv(props, "telegram.chat_id", "TELEGRAM_CHAT_ID");
        overrideFromEnv(props, "telegram.api_url", "TELEGRAM_API_URL");
        return props;
    }

    private static void overrideFromEnv(Properties props, String key, String envVar) {
        String value = System.getenv(envVar);
        if (value != null && !value.isBlank()) {
            props.setProperty(key, value);
        }
    }
}
