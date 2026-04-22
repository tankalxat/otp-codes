package ru.example.otpcodes.sender;

import jakarta.mail.Authenticator;
import jakarta.mail.Message;
import jakarta.mail.MessagingException;
import jakarta.mail.PasswordAuthentication;
import jakarta.mail.Session;
import jakarta.mail.Transport;
import jakarta.mail.internet.InternetAddress;
import jakarta.mail.internet.MimeMessage;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Component;
import ru.example.otpcodes.domain.Channel;

import java.io.IOException;
import java.io.InputStream;
import java.util.Properties;

@Slf4j
@Component
public class EmailSender implements NotificationSender {

    private final String username;
    private final String password;
    private final String fromEmail;
    private final Session session;

    public EmailSender() {
        Properties config = loadConfig();
        this.username = config.getProperty("email.username");
        this.password = config.getProperty("email.password");
        this.fromEmail = config.getProperty("email.from");
        boolean authEnabled = Boolean.parseBoolean(config.getProperty("mail.smtp.auth", "false"));
        if (authEnabled) {
            this.session = Session.getInstance(config, new Authenticator() {
                @Override
                protected PasswordAuthentication getPasswordAuthentication() {
                    return new PasswordAuthentication(username, password);
                }
            });
        } else {
            this.session = Session.getInstance(config);
        }
    }

    @Override
    public Channel channel() {
        return Channel.EMAIL;
    }

    @Override
    public void send(String destination, String code) {
        try {
            Message message = new MimeMessage(session);
            message.setFrom(new InternetAddress(fromEmail));
            message.setRecipient(Message.RecipientType.TO, new InternetAddress(destination));
            message.setSubject("Your OTP Code");
            message.setText("Your verification code is: " + code);
            Transport.send(message);
            log.info("Email with OTP sent to {}", destination);
        } catch (MessagingException e) {
            log.error("Failed to send email to {}: {}", destination, e.getMessage(), e);
            throw new RuntimeException("Failed to send email", e);
        }
    }

    private Properties loadConfig() {
        Properties props = new Properties();
        try (InputStream is = EmailSender.class.getClassLoader().getResourceAsStream("email.properties")) {
            if (is == null) {
                throw new IllegalStateException("email.properties not found on classpath");
            }
            props.load(is);
        } catch (IOException e) {
            throw new RuntimeException("Failed to load email configuration", e);
        }
        overrideFromEnv(props, "email.username", "EMAIL_USERNAME");
        overrideFromEnv(props, "email.password", "EMAIL_PASSWORD");
        overrideFromEnv(props, "email.from", "EMAIL_FROM");
        overrideFromEnv(props, "mail.smtp.host", "SMTP_HOST");
        overrideFromEnv(props, "mail.smtp.port", "SMTP_PORT");
        overrideFromEnv(props, "mail.smtp.auth", "SMTP_AUTH");
        overrideFromEnv(props, "mail.smtp.starttls.enable", "SMTP_STARTTLS");
        return props;
    }

    private static void overrideFromEnv(Properties props, String key, String envVar) {
        String value = System.getenv(envVar);
        if (value != null && !value.isBlank()) {
            props.setProperty(key, value);
        }
    }
}
