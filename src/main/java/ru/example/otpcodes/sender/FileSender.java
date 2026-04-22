package ru.example.otpcodes.sender;

import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;
import ru.example.otpcodes.domain.Channel;

import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.StandardOpenOption;
import java.time.OffsetDateTime;

@Slf4j
@Component
public class FileSender implements NotificationSender {

    private final Path filePath;

    public FileSender(@Value("${otp.file.path}") String path) {
        this.filePath = Path.of(path);
    }

    @Override
    public Channel channel() {
        return Channel.FILE;
    }

    @Override
    public void send(String destination, String code) {
        String line = String.format("%s | destination=%s | code=%s%n",
                OffsetDateTime.now(), destination == null ? "-" : destination, code);
        try {
            Files.writeString(filePath, line, StandardCharsets.UTF_8,
                    StandardOpenOption.CREATE, StandardOpenOption.APPEND);
            log.info("OTP code written to file {}", filePath);
        } catch (IOException e) {
            log.error("Failed to write OTP to file {}: {}", filePath, e.getMessage(), e);
            throw new RuntimeException("Failed to write OTP to file", e);
        }
    }
}
